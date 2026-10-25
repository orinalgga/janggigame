package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameState;
import com.janggi.core.IllegalMoveException;
import com.janggi.core.Move;
import com.janggi.core.Side;
import java.util.Random;

/** 대국 하나. AI 대국이면 사람이 둔 뒤 같은 요청 안에서 AI가 이어서 둔다. */
final class GameSession {

    private final Side aiSide;
    private final Engine.Level aiLevel;
    private GameState state;
    private Move lastMove;

    GameSession(GameState initial, Side aiSide, Engine.Level aiLevel) {
        this.state = initial;
        this.aiSide = aiSide;
        this.aiLevel = aiLevel;
        playAiIfItsTurn();
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
        lastMove = move;
    }
}
