package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.GameState;
import com.janggi.core.IllegalMoveException;
import com.janggi.core.Move;
import com.janggi.core.Side;
import java.util.Objects;
import java.util.Random;

/**
 * 진행 중인 대국 하나. AI 대국이면 사람이 둔 뒤 같은 요청 안에서 AI가 이어서 둔다.
 * 시계가 있으면 온라인 대국으로, 회원 확인·시간패·기권이 붙는다.
 */
final class GameSession {

    interface Listener {
        /** 수를 둘 때마다 불린다. seq는 1부터 센 수 번호. clock은 온라인 대국에서만 있다. */
        void onMove(int seq, Move move, GameState after, GameClock.Snapshot clock);

        /** 대국이 끝나면 한 번 불린다. 온라인 대국이면 바뀐 레이팅을 돌려준다. */
        RatingChange onEnd(GameResult result, String reason);
    }

    /** 각 진영을 두는 회원. AI나 비회원 쪽은 null. */
    record Players(GameDto.PlayerDto cho, GameDto.PlayerDto han) {

        static final Players NONE = new Players(null, null);

        Long userId(Side side) {
            GameDto.PlayerDto p = side == Side.CHO ? cho : han;
            return p == null ? null : p.id();
        }
    }

    private final Side aiSide;
    private final Engine.Level aiLevel;
    private final Players players;
    private final GameClock clock;
    private final Listener listener;
    private GameState state;
    private int moveCount;
    private Move lastMove;
    private GameResult endResult;
    private String endReason;
    private RatingChange ratingChange;
    /** 온라인 대국에서 각 진영(초, 한) 회원이 대국 화면에 마지막으로 보인 때. */
    private final long[] lastSeen = new long[2];
    /** 각 진영이 지금 '나간 것'으로 보이는지. 바뀔 때만 두 사람에게 알린다. */
    private final boolean[] away = new boolean[2];

    GameSession(GameState state, Side aiSide, Engine.Level aiLevel, Players players, GameClock clock,
            int moveCount, Move lastMove, Listener listener) {
        this.state = state;
        this.aiSide = aiSide;
        this.aiLevel = aiLevel;
        this.players = players;
        this.clock = clock;
        this.moveCount = moveCount;
        this.lastMove = lastMove;
        this.listener = listener;
        // 대국을 만들거나 서버가 다시 켜진 직후에는 둘 다 막 들어오는 중이라고 본다.
        long now = now();
        lastSeen[0] = now;
        lastSeen[1] = now;
        if (state.isGameOver()) {
            endResult = state.result();
            endReason = reasonOf(state);
        }
    }

    synchronized GameState state() {
        return state;
    }

    synchronized Move lastMove() {
        return lastMove;
    }

    synchronized GameResult result() {
        return endResult == null ? GameResult.ONGOING : endResult;
    }

    synchronized String endReason() {
        return endReason;
    }

    synchronized RatingChange ratingChange() {
        return ratingChange;
    }

    synchronized GameClock.Snapshot clock() {
        return clock == null ? null : clock.snapshot(now());
    }

    synchronized boolean isOver() {
        return endResult != null;
    }

    boolean online() {
        return clock != null;
    }

    Players players() {
        return players;
    }

    Side aiSide() {
        return aiSide;
    }

    Engine.Level aiLevel() {
        return aiLevel;
    }

    /** 기권·시간패로 이미 끝난 대국을 복원할 때 그 결과를 되살린다. */
    synchronized void restoreEnd(GameResult result, String reason, RatingChange change) {
        endResult = result;
        endReason = reason;
        ratingChange = change;
        if (clock != null) {
            clock.stop(now());
        }
    }

    /** AI가 둘 차례면 둔다. 새 대국을 만들거나 복원한 직후에 부른다. */
    synchronized void start() {
        playAiIfItsTurn();
    }

    /** userId는 온라인 대국에서 둘 차례인 회원인지 확인하는 데만 쓴다. */
    synchronized void playHuman(Move move, Long userId) {
        if (isOver() || state.sideToMove() == aiSide) {
            throw new IllegalMoveException(move);
        }
        if (online()) {
            if (userId == null || !Objects.equals(players.userId(state.sideToMove()), userId)) {
                throw new NotYourGameException();
            }
            if (clock.flagged(now())) {
                // 시간이 다 된 뒤에 도착한 수는 두지 않고 시간패로 끝낸다.
                timeout();
                return;
            }
        }
        apply(move);
        playAiIfItsTurn();
    }

    /** 온라인 대국은 두는 회원만, AI 대국은 사람 쪽이 기권한다. 같은 화면 2인 대국에는 기권이 없다. */
    synchronized void resign(Long userId) {
        if (!online() && aiSide != null) {
            if (!isOver()) {
                end(winnerAgainst(aiSide.opponent()), "RESIGN");
            }
            return;
        }
        if (!online() || userId == null) {
            throw new NotYourGameException();
        }
        Side side = userId.equals(players.userId(Side.CHO)) ? Side.CHO
                : userId.equals(players.userId(Side.HAN)) ? Side.HAN
                : null;
        if (side == null) {
            throw new NotYourGameException();
        }
        if (isOver()) {
            return;
        }
        clock.stop(now());
        end(winnerAgainst(side), "RESIGN");
    }

    /** 둘 차례인 쪽 시간이 다 됐으면 시간패로 끝낸다. 끝냈으면 true. */
    synchronized boolean checkTimeout() {
        if (!online() || isOver() || !clock.flagged(now())) {
            return false;
        }
        timeout();
        return true;
    }

    /**
     * 각 진영 회원이 대국 화면을 보고 있는지 알려 준다(온라인 대국). noticeMs보다 오래 안 보이면 '나감'으로 표시하고,
     * graceMs가 지나도록 돌아오지 않으면 남은 쪽의 승리로 끝낸다. 둘 다 나갔으면 끝내지 않고 시계에 맡긴다.
     * 두 사람에게 알릴 만큼 바뀌었으면 true.
     */
    synchronized boolean updatePresence(boolean choHere, boolean hanHere, long now, long noticeMs, long graceMs) {
        if (!online() || isOver()) {
            return false;
        }
        boolean changed = false;
        boolean[] here = {choHere, hanHere};
        for (int i = 0; i < 2; i++) {
            if (here[i]) {
                lastSeen[i] = now;
            }
            boolean nowAway = now - lastSeen[i] >= noticeMs;
            if (nowAway != away[i]) {
                away[i] = nowAway;
                changed = true;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (away[i] && !away[1 - i] && now - lastSeen[i] >= graceMs) {
                clock.stop(now);
                end(winnerAgainst(i == 0 ? Side.CHO : Side.HAN), "DISCONNECT");
                return true;
            }
        }
        return changed;
    }

    /** 이 진영이 나간 것으로 보이면 돌아오기까지 남은 시간(밀리초), 아니면 null. */
    synchronized Long awayMsLeft(Side side, long graceMs) {
        int i = side == Side.CHO ? 0 : 1;
        if (!online() || isOver() || !away[i]) {
            return null;
        }
        return Math.max(0, lastSeen[i] + graceMs - now());
    }

    private void timeout() {
        Side loser = state.sideToMove();
        clock.stop(now());
        end(winnerAgainst(loser), "TIMEOUT");
    }

    private void playAiIfItsTurn() {
        if (aiSide != null && state.sideToMove() == aiSide && !isOver()) {
            apply(new Engine(new Random()).bestMove(state, aiLevel));
        }
    }

    private void apply(Move move) {
        state = state.apply(move);
        moveCount++;
        lastMove = move;
        GameClock.Snapshot snapshot = null;
        if (clock != null) {
            long now = now();
            if (state.isGameOver()) {
                clock.stop(now);
            } else {
                clock.punch(now);
            }
            snapshot = clock.snapshot(now);
        }
        listener.onMove(moveCount, move, state, snapshot);
        if (state.isGameOver()) {
            end(state.result(), reasonOf(state));
        }
    }

    private void end(GameResult result, String reason) {
        endResult = result;
        endReason = reason;
        ratingChange = listener.onEnd(result, reason);
    }

    private static String reasonOf(GameState state) {
        if (state.isCheckmate()) {
            return "CHECKMATE";
        }
        return state.isBikjangOver() ? "BIKJANG" : "MATERIAL";
    }

    private static GameResult winnerAgainst(Side loser) {
        return loser == Side.CHO ? GameResult.HAN_WIN : GameResult.CHO_WIN;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
