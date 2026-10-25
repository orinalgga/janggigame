package com.janggi.core;

public enum PieceType {
    GENERAL("장", 0),
    GUARD("사", 3),
    ELEPHANT("상", 3),
    HORSE("마", 5),
    CHARIOT("차", 13),
    CANNON("포", 7),
    SOLDIER("졸", 2);

    private final String korean;
    private final int points;

    PieceType(String korean, int points) {
        this.korean = korean;
        this.points = points;
    }

    public String korean() {
        return korean;
    }

    /** 점수제 판정에 쓰는 기물 점수. */
    public int points() {
        return points;
    }
}
