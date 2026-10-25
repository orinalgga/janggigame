package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.Side;
import com.janggi.server.record.GameRecord;
import java.time.OffsetDateTime;

public record GameSummaryDto(
        String id,
        Side aiSide,
        Engine.Level aiLevel,
        GameResult result,
        String endReason,
        int moveCount,
        OffsetDateTime startedAt,
        boolean online) {

    static GameSummaryDto from(GameRecord g) {
        return new GameSummaryDto(g.getId().toString(), g.getAiSide(), g.getAiLevel(), g.getResult(),
                g.getEndReason(), g.getMoveCount(), g.getStartedAt(), g.isOnline());
    }
}
