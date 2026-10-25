package com.janggi.core;

import java.util.List;

/** 궁성(x 3~5, 초 y 0~2, 한 y 7~9)과 궁성 안 대각선. */
public final class Palace {

    private Palace() {
    }

    public static boolean contains(Square s) {
        return s.x() >= 3 && s.x() <= 5 && ((s.y() >= 0 && s.y() <= 2) || (s.y() >= 7 && s.y() <= 9));
    }

    public static boolean ownedBy(Square s, Side side) {
        if (!contains(s)) {
            return false;
        }
        return side == Side.CHO ? s.y() <= 2 : s.y() >= 7;
    }

    private static int centerY(Square s) {
        return s.y() <= 2 ? 1 : 8;
    }

    public static boolean isCenter(Square s) {
        return contains(s) && s.x() == 4 && s.y() == centerY(s);
    }

    public static boolean isCorner(Square s) {
        return contains(s) && s.x() != 4 && s.y() != centerY(s);
    }

    public static Square center(Square s) {
        return Square.of(4, centerY(s));
    }

    /** 대각선으로 한 칸 이어진 칸들. 중앙이면 네 귀퉁이, 귀퉁이면 중앙, 그 외에는 없음. */
    public static List<Square> diagonalNeighbors(Square s) {
        if (isCenter(s)) {
            int cy = s.y();
            return List.of(Square.of(3, cy - 1), Square.of(5, cy - 1), Square.of(3, cy + 1), Square.of(5, cy + 1));
        }
        if (isCorner(s)) {
            return List.of(center(s));
        }
        return List.of();
    }

    /** 귀퉁이에서 중앙을 지나 반대편 귀퉁이. 귀퉁이가 아니면 null. */
    public static Square oppositeCorner(Square s) {
        if (!isCorner(s)) {
            return null;
        }
        Square c = center(s);
        return Square.of(2 * c.x() - s.x(), 2 * c.y() - s.y());
    }
}
