package com.janggi.core;

/** 국면 평가. 기물 점수(덤 포함)에 위치 보너스를 조금 더한다. 수를 둘 차례인 쪽 기준. */
final class Evaluator {

    private Evaluator() {
    }

    static int evaluate(GameState state) {
        Side me = state.sideToMove();
        return sideValue(state, me) - sideValue(state, me.opponent());
    }

    private static int sideValue(GameState state, Side side) {
        Board board = state.boardView();
        int total = (int) Math.round(state.score(side) * 100);
        for (Square s : board.squaresOf(side)) {
            Piece p = board.get(s);
            int advance = side == Side.CHO ? s.y() : 9 - s.y();
            total += switch (p.type()) {
                // 강을 건너 앞으로 나간 졸은 상대 궁성을 위협한다.
                case SOLDIER -> advance >= 5 ? 20 + 10 * (advance - 5) : 0;
                // 마와 상은 가장자리보다 가운데에서 갈 곳이 많다.
                case HORSE, ELEPHANT -> 12 - 4 * Math.abs(s.x() - 4);
                case CHARIOT, CANNON -> advance >= 3 && advance <= 7 ? 15 : 0;
                default -> 0;
            };
        }
        return total;
    }
}
