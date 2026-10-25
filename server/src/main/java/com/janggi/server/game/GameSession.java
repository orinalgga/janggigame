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

    synchronized void resign(Long userId) {
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
        return state.isCheckmate() ? "CHECKMATE" : "BIKJANG";
    }

    private static GameResult winnerAgainst(Side loser) {
        return loser == Side.CHO ? GameResult.HAN_WIN : GameResult.CHO_WIN;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
