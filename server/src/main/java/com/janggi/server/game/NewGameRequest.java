package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.Side;

/** aiSide를 비우면 2인 대국, 넣으면 그쪽을 AI가 둔다. */
public record NewGameRequest(Formation choFormation, Formation hanFormation, Side aiSide, Engine.Level aiLevel) {

    Formation cho() {
        return choFormation == null ? Formation.MA_SANG_MA_SANG : choFormation;
    }

    Formation han() {
        return hanFormation == null ? Formation.MA_SANG_MA_SANG : hanFormation;
    }

    Engine.Level level() {
        return aiLevel == null ? Engine.Level.INTERMEDIATE : aiLevel;
    }
}
