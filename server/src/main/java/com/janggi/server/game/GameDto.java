package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import com.janggi.core.Square;
import com.janggi.server.chat.ChatMessage;
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
        MoveDto lastMove,
        String endReason,
        boolean online,
        PlayerDto cho,
        PlayerDto han,
        ClockDto clock,
        RatingChange ratingChange,
        ChatMessage aiComment) {

    /** rating은 대국을 시작할 때의 레이팅. */
    public record PlayerDto(long id, String nickname, int rating) {
    }

    /** 남은 시간(밀리초)과 지금 시계가 가는 쪽. 웹은 받은 뒤부터 running 쪽을 직접 줄여 보여 준다. */
    public record ClockDto(long choMs, long hanMs, Side running) {

        static ClockDto from(GameClock.Snapshot s) {
            return s == null ? null : new ClockDto(s.choMs(), s.hanMs(), s.running());
        }
    }

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
        // 수와 시계, 결과가 한 시점의 것이 되도록 세션을 잡고 만든다.
        synchronized (session) {
            return build(id, session);
        }
    }

    private static GameDto build(String id, GameSession session) {
        GameState state = session.state();
        boolean over = session.isOver();
        return new GameDto(
                id,
                pieces(state),
                state.sideToMove(),
                over ? List.of() : state.legalMoves().stream().map(MoveDto::from).toList(),
                state.isCheck(),
                state.isCheckmate(),
                state.isBikjang(),
                session.result(),
                state.score(Side.CHO),
                state.score(Side.HAN),
                session.aiSide(),
                session.aiLevel(),
                session.lastMove() == null ? null : MoveDto.from(session.lastMove()),
                session.endReason(),
                session.online(),
                session.players().cho(),
                session.players().han(),
                ClockDto.from(session.clock()),
                session.ratingChange(),
                null);
    }

    /** AI 대국에서 이번 수에 AI가 한 말을 붙인다. */
    GameDto withAiComment(ChatMessage comment) {
        return new GameDto(id, pieces, sideToMove, legalMoves, check, checkmate, bikjang, result, choScore, hanScore,
                aiSide, aiLevel, lastMove, endReason, online, cho, han, clock, ratingChange, comment);
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
