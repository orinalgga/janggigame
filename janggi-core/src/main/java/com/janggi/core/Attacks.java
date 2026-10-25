package com.janggi.core;

/**
 * 장이 공격받는지를 장 자리에서 거꾸로 살펴 빠르게 판단한다.
 * 상대 수를 모두 만들어 보는 것보다 훨씬 빨라서 AI 탐색에 필요하다.
 *
 * <p>장과 사는 자기 궁성을 벗어나지 못하고 두 궁성은 떨어져 있으므로, 상대 장·사가
 * 우리 장을 공격하는 일은 없다. 그래서 차·포·마·상·졸만 본다.
 */
final class Attacks {

    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] HORSE = {{1, 2}, {-1, 2}, {1, -2}, {-1, -2}, {2, 1}, {-2, 1}, {2, -1}, {-2, -1}};
    private static final int[][] ELEPHANT = {{2, 3}, {-2, 3}, {2, -3}, {-2, -3}, {3, 2}, {-3, 2}, {3, -2}, {-3, -2}};

    private Attacks() {
    }

    /** bySide의 기물이 target(장이 있는 칸)을 잡을 수 있으면 true. */
    static boolean onGeneral(Board board, Square target, Side bySide) {
        return byLines(board, target, bySide)
                || byPalaceDiagonals(board, target, bySide)
                || byHorse(board, target, bySide)
                || byElephant(board, target, bySide)
                || bySoldier(board, target, bySide);
    }

    private static boolean isEnemy(Piece p, Side bySide, PieceType type) {
        return p != null && p.side() == bySide && p.type() == type;
    }

    /** 차는 첫 기물, 포는 포가 아닌 기물 하나 너머의 첫 기물. */
    private static boolean byLines(Board board, Square target, Side bySide) {
        for (int[] d : ORTHOGONAL) {
            Square s = target.offset(d[0], d[1]);
            while (s.onBoard() && board.isEmpty(s)) {
                s = s.offset(d[0], d[1]);
            }
            if (!s.onBoard()) {
                continue;
            }
            Piece first = board.get(s);
            if (isEnemy(first, bySide, PieceType.CHARIOT)) {
                return true;
            }
            if (first.is(PieceType.CANNON)) {
                continue;
            }
            s = s.offset(d[0], d[1]);
            while (s.onBoard() && board.isEmpty(s)) {
                s = s.offset(d[0], d[1]);
            }
            if (s.onBoard() && isEnemy(board.get(s), bySide, PieceType.CANNON)) {
                return true;
            }
        }
        return false;
    }

    private static boolean byPalaceDiagonals(Board board, Square target, Side bySide) {
        if (Palace.isCenter(target)) {
            for (Square n : Palace.diagonalNeighbors(target)) {
                if (isEnemy(board.get(n), bySide, PieceType.CHARIOT)) {
                    return true;
                }
            }
            return false;
        }
        if (!Palace.isCorner(target)) {
            return false;
        }
        Piece mid = board.get(Palace.center(target));
        Piece far = board.get(Palace.oppositeCorner(target));
        if (mid == null) {
            return isEnemy(far, bySide, PieceType.CHARIOT);
        }
        return isEnemy(mid, bySide, PieceType.CHARIOT)
                || (!mid.is(PieceType.CANNON) && isEnemy(far, bySide, PieceType.CANNON));
    }

    private static boolean byHorse(Board board, Square target, Side bySide) {
        for (int[] o : HORSE) {
            Square from = target.offset(-o[0], -o[1]);
            if (!from.onBoard() || !isEnemy(board.get(from), bySide, PieceType.HORSE)) {
                continue;
            }
            Square leg = Math.abs(o[1]) == 2 ? from.offset(0, Integer.signum(o[1])) : from.offset(Integer.signum(o[0]), 0);
            if (board.isEmpty(leg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean byElephant(Board board, Square target, Side bySide) {
        for (int[] o : ELEPHANT) {
            Square from = target.offset(-o[0], -o[1]);
            if (!from.onBoard() || !isEnemy(board.get(from), bySide, PieceType.ELEPHANT)) {
                continue;
            }
            int sx = Integer.signum(o[0]), sy = Integer.signum(o[1]);
            Square leg = Math.abs(o[1]) == 3 ? from.offset(0, sy) : from.offset(sx, 0);
            Square mid = leg.offset(sx, sy);
            if (board.isEmpty(leg) && board.isEmpty(mid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean bySoldier(Board board, Square target, Side bySide) {
        int f = bySide.forward();
        Square[] froms = {target.offset(0, -f), target.offset(1, 0), target.offset(-1, 0)};
        for (Square from : froms) {
            if (from.onBoard() && isEnemy(board.get(from), bySide, PieceType.SOLDIER)) {
                return true;
            }
        }
        for (Square from : Palace.diagonalNeighbors(target)) {
            if (target.y() - from.y() == f && isEnemy(board.get(from), bySide, PieceType.SOLDIER)) {
                return true;
            }
        }
        return false;
    }
}
