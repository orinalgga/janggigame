package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameState;
import com.janggi.core.IllegalMoveException;
import com.janggi.core.Move;
import com.janggi.core.Side;
import java.util.Random;

/** 진행 중인 대국 하나. AI 대국이면 사람이 둔 뒤 같은 요청 안에서 AI가 이어서 둔다. */
final class GameSession {

    /** 수를 둘 때마다 불린다. seq는 1부터 센 수 번호. */
    interface MoveListener {
        void onMove(int seq, Move move, GameState after);
    }

    private final Side aiSide;
    private final Engine.Level aiLevel;
    private final MoveListener listener;
    private GameState state;
    private int moveCount;
    private Move lastMove;

    GameSession(GameState state, Side aiSide, Engine.Level aiLevel, int moveCount, Move lastMove, MoveListener listener) {
        this.state = state;
        this.aiSide = aiSide;
        this.aiLevel = aiLevel;
        this.moveCount = moveCount;
        this.lastMove = lastMove;
        this.listener = listener;
    }

    synchronized GameState state() {
        return state;
    }

    synchronized Move lastMove() {
        return lastMove;
    }

    Side aiSide() {
        return aiSide;
    }

    Engine.Level aiLevel() {
        return aiLevel;
    }

    /** AI가 둘 차례면 둔다. 새 대국을 만들거나 복원한 직후에 부른다. */
    synchronized void start() {
        playAiIfItsTurn();
    }

    synchronized void playHuman(Move move) {
        if (state.sideToMove() == aiSide) {
            throw new IllegalMoveException(move);
        }
        apply(move);
        playAiIfItsTurn();
    }

    private void playAiIfItsTurn() {
        if (aiSide != null && state.sideToMove() == aiSide && !state.isGameOver()) {
            apply(new Engine(new Random()).bestMove(state, aiLevel));
        }
    }

    private void apply(Move move) {
        state = state.apply(move);
        moveCount++;
        lastMove = move;
        listener.onMove(moveCount, move, state);
    }
}
