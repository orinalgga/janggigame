package com.janggi.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 기물별 이동 규칙. 여기서 만드는 수는 "자기 장이 잡히는지"를 따지지 않은 수다.
 * 그 검사는 {@link GameState#legalMoves()}가 한다.
 */
public final class MoveGenerator {

    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private MoveGenerator() {
    }

    public static List<Move> pseudoLegalMoves(Board board, Side side) {
        List<Move> moves = new ArrayList<>();
        for (Square from : board.squaresOf(side)) {
            movesFrom(board, from, moves);
        }
        return moves;
    }

    public static List<Move> pseudoLegalMovesFrom(Board board, Square from) {
        List<Move> moves = new ArrayList<>();
        movesFrom(board, from, moves);
        return moves;
    }

    /** bySide의 기물 중 하나라도 target으로 갈 수 있으면 true. */
    public static boolean isAttacked(Board board, Square target, Side bySide) {
        for (Move m : pseudoLegalMoves(board, bySide)) {
            if (m.to().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static void movesFrom(Board board, Square from, List<Move> out) {
        Piece piece = board.get(from);
        if (piece == null) {
            return;
        }
        switch (piece.type()) {
            case GENERAL, GUARD -> palaceStep(board, from, piece, out);
            case CHARIOT -> chariot(board, from, piece, out);
            case CANNON -> cannon(board, from, piece, out);
            case HORSE -> horse(board, from, piece, out);
            case ELEPHANT -> elephant(board, from, piece, out);
            case SOLDIER -> soldier(board, from, piece, out);
        }
    }

    private static boolean canLand(Board board, Square to, Piece mover) {
        if (!to.onBoard()) {
            return false;
        }
        Piece target = board.get(to);
        return target == null || target.side() != mover.side();
    }

    private static void add(Board board, Square from, Square to, Piece mover, List<Move> out) {
        if (canLand(board, to, mover)) {
            out.add(new Move(from, to));
        }
    }

    /** 장·사: 자기 궁성 안에서 선이나 대각선을 따라 한 칸. */
    private static void palaceStep(Board board, Square from, Piece piece, List<Move> out) {
        for (int[] d : ORTHOGONAL) {
            Square to = from.offset(d[0], d[1]);
            if (Palace.ownedBy(to, piece.side())) {
                add(board, from, to, piece, out);
            }
        }
        for (Square to : Palace.diagonalNeighbors(from)) {
            add(board, from, to, piece, out);
        }
    }

    /** 차: 직선으로 몇 칸이든. 궁성 안에서는 대각선으로도. */
    private static void chariot(Board board, Square from, Piece piece, List<Move> out) {
        for (int[] d : ORTHOGONAL) {
            Square to = from.offset(d[0], d[1]);
            while (to.onBoard()) {
                if (board.isEmpty(to)) {
                    out.add(new Move(from, to));
                } else {
                    add(board, from, to, piece, out);
                    break;
                }
                to = to.offset(d[0], d[1]);
            }
        }
        if (Palace.isCenter(from)) {
            for (Square to : Palace.diagonalNeighbors(from)) {
                add(board, from, to, piece, out);
            }
        } else if (Palace.isCorner(from)) {
            Square center = Palace.center(from);
            if (board.isEmpty(center)) {
                out.add(new Move(from, center));
                add(board, from, Palace.oppositeCorner(from), piece, out);
            } else {
                add(board, from, center, piece, out);
            }
        }
    }

    /** 포: 포가 아닌 기물 하나를 넘어서 간다. 포는 넘을 수도, 잡을 수도 없다. */
    private static void cannon(Board board, Square from, Piece piece, List<Move> out) {
        for (int[] d : ORTHOGONAL) {
            Square to = from.offset(d[0], d[1]);
            while (to.onBoard() && board.isEmpty(to)) {
                to = to.offset(d[0], d[1]);
            }
            if (!to.onBoard() || board.get(to).is(PieceType.CANNON)) {
                continue;
            }
            to = to.offset(d[0], d[1]);
            while (to.onBoard()) {
                Piece target = board.get(to);
                if (target == null) {
                    out.add(new Move(from, to));
                } else {
                    if (target.side() != piece.side() && !target.is(PieceType.CANNON)) {
                        out.add(new Move(from, to));
                    }
                    break;
                }
                to = to.offset(d[0], d[1]);
            }
        }
        if (Palace.isCorner(from)) {
            Piece screen = board.get(Palace.center(from));
            Square to = Palace.oppositeCorner(from);
            Piece target = board.get(to);
            if (screen != null && !screen.is(PieceType.CANNON)
                    && (target == null || (target.side() != piece.side() && !target.is(PieceType.CANNON)))) {
                out.add(new Move(from, to));
            }
        }
    }

    /** 마: 직선 한 칸 뒤 바깥쪽 대각선 한 칸. 첫 칸이 막히면 못 간다. */
    private static void horse(Board board, Square from, Piece piece, List<Move> out) {
        for (int[] d : ORTHOGONAL) {
            Square leg = from.offset(d[0], d[1]);
            if (!leg.onBoard() || !board.isEmpty(leg)) {
                continue;
            }
            int px = d[1], py = d[0];
            add(board, from, leg.offset(d[0] + px, d[1] + py), piece, out);
            add(board, from, leg.offset(d[0] - px, d[1] - py), piece, out);
        }
    }

    /** 상: 직선 한 칸 뒤 대각선 두 칸. 지나는 두 칸 중 하나라도 막히면 못 간다. */
    private static void elephant(Board board, Square from, Piece piece, List<Move> out) {
        for (int[] d : ORTHOGONAL) {
            Square leg = from.offset(d[0], d[1]);
            if (!leg.onBoard() || !board.isEmpty(leg)) {
                continue;
            }
            int px = d[1], py = d[0];
            for (int sign : new int[] {1, -1}) {
                int dx = d[0] + sign * px, dy = d[1] + sign * py;
                Square mid = leg.offset(dx, dy);
                if (mid.onBoard() && board.isEmpty(mid)) {
                    add(board, from, mid.offset(dx, dy), piece, out);
                }
            }
        }
    }

    /** 졸·병: 앞이나 옆으로 한 칸. 궁성 안에서는 앞쪽 대각선으로도. */
    private static void soldier(Board board, Square from, Piece piece, List<Move> out) {
        int f = piece.side().forward();
        add(board, from, from.offset(0, f), piece, out);
        add(board, from, from.offset(1, 0), piece, out);
        add(board, from, from.offset(-1, 0), piece, out);
        for (Square to : Palace.diagonalNeighbors(from)) {
            if (to.y() - from.y() == f) {
                add(board, from, to, piece, out);
            }
        }
    }
}
