package com.janggi.server.game;

import com.janggi.core.Formation;

public record NewGameRequest(Formation choFormation, Formation hanFormation) {

    Formation cho() {
        return choFormation == null ? Formation.MA_SANG_MA_SANG : choFormation;
    }

    Formation han() {
        return hanFormation == null ? Formation.MA_SANG_MA_SANG : hanFormation;
    }
}
