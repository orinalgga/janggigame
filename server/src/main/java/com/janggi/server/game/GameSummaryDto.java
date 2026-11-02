package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.Side;
import com.janggi.server.record.GameRecord;
import java.time.OffsetDateTime;

/**
 * mySide는 목록을 본 회원이 둔 진영(구경·비회원·같은 화면 2인이면 null)이라 화면이 내 기준 승패를 보여 줄 수 있다.
 * opponentNickname은 온라인 대국 상대의 닉네임.
 */
public record GameSummaryDto(
        String id,
        Side aiSide,
        Engine.Level aiLevel,
        GameResult result,
        String endReason,
        int moveCount,
        OffsetDateTime startedAt,
        boolean online,
        Side mySide,
        String opponentNickname) {

    public static GameSummaryDto from(GameRecord g, Long viewerId, String opponentNickname) {
        return new GameSummaryDto(g.getId().toString(), g.getAiSide(), g.getAiLevel(), g.getResult(),
                g.getEndReason(), g.getMoveCount(), g.getStartedAt(), g.isOnline(), sideOf(g, viewerId),
                opponentNickname);
    }

    /** 같은 회원이 양쪽에 다 있으면(같은 화면 2인) 어느 쪽인지 정할 수 없다. */
    public static Side sideOf(GameRecord g, Long viewerId) {
        if (viewerId == null) {
            return null;
        }
        boolean cho = viewerId.equals(g.getChoUserId());
        boolean han = viewerId.equals(g.getHanUserId());
        if (cho == han) {
            return null;
        }
        return cho ? Side.CHO : Side.HAN;
    }
}
