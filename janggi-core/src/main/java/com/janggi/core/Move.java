package com.janggi.core;

/** 한 수. from과 to가 모두 null이면 한수쉼(패스)이다. */
public record Move(Square from, Square to) {

    private static final Move PASS = new Move(null, null);

    public static Move of(int fx, int fy, int tx, int ty) {
        return new Move(Square.of(fx, fy), Square.of(tx, ty));
    }

    public static Move pass() {
        return PASS;
    }

    public boolean isPass() {
        return from == null && to == null;
    }

    @Override
    public String toString() {
        return isPass() ? "pass" : from + "->" + to;
    }
}
