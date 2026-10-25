package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import com.janggi.core.Square;
import java.util.ArrayList;
import java.util.List;

public record GameDto(
        String id,
        List<PieceDto> pieces,
        Side sideToMove,
        List<MoveDto> legalMoves,
        boolean check,
        boolean checkmate,
        boolean bikjang,
        GameResult result,
        double choScore,
        double hanScore,
        Side aiSide,
        Engine.Level aiLevel,
        MoveDto lastMove) {

    public record PieceDto(int x, int y, String side, String type, String label) {
    }

    public record SquareDto(int x, int y) {

        Square toSquare() {
            return Square.of(x, y);
        }

        static SquareDto from(Square s) {
            return s == null ? null : new SquareDto(s.x(), s.y());
        }
    }

    /** from과 to가 모두 null이면 한수쉼. */
    public record MoveDto(SquareDto from, SquareDto to) {

        Move toMove() {
            return from == null && to == null ? Move.pass() : new Move(from.toSquare(), to.toSquare());
        }

        static MoveDto from(Move m) {
            return new MoveDto(SquareDto.from(m.from()), SquareDto.from(m.to()));
        }
    }

    static GameDto from(String id, GameSession session) {
        GameState state = session.state();
        return new GameDto(
                id,
                pieces(state),
                state.sideToMove(),
                state.legalMoves().stream().map(MoveDto::from).toList(),
                state.isCheck(),
                state.isCheckmate(),
                state.isBikjang(),
                state.result(),
                state.score(Side.CHO),
                state.score(Side.HAN),
                session.aiSide(),
                session.aiLevel(),
                session.lastMove() == null ? null : MoveDto.from(session.lastMove()));
    }

    static List<PieceDto> pieces(GameState state) {
        List<PieceDto> pieces = new ArrayList<>();
        for (int y = 0; y < Square.RANKS; y++) {
            for (int x = 0; x < Square.FILES; x++) {
                var p = state.pieceAt(Square.of(x, y));
                if (p != null) {
                    pieces.add(new PieceDto(x, y, p.side().name(), p.type().name(), label(p.side(), p.type())));
                }
            }
        }
        return pieces;
    }

    /** 장기알에 쓰는 한자. 초는 장(將)이 아닌 초(楚), 한은 한(漢)을 쓴다. */
    private static String label(Side side, com.janggi.core.PieceType type) {
        boolean cho = side == Side.CHO;
        return switch (type) {
            case GENERAL -> cho ? "楚" : "漢";
            case GUARD -> "士";
            case ELEPHANT -> "象";
            case HORSE -> "馬";
            case CHARIOT -> "車";
            case CANNON -> "包";
            case SOLDIER -> cho ? "卒" : "兵";
        };
    }
}
