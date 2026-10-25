package com.janggi.server.game;

import com.janggi.core.GameResult;
import com.janggi.core.Side;
import java.util.List;

/** 복기용 한 장면. ply번째 수까지 둔 국면이다. */
public record ReplayDto(
        String id,
        int ply,
        int totalPlies,
        List<GameDto.PieceDto> pieces,
        GameDto.MoveDto lastMove,
        boolean check,
        GameResult result,
        Side aiSide) {
}
