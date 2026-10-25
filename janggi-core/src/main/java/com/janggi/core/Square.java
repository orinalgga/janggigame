package com.janggi.core;

/** 장기판 좌표. x는 0~8(세로줄), y는 0~9(가로줄), 초 쪽이 y=0. */
public record Square(int x, int y) {

    public static final int FILES = 9;
    public static final int RANKS = 10;

    public static Square of(int x, int y) {
        return new Square(x, y);
    }

    public boolean onBoard() {
        return x >= 0 && x < FILES && y >= 0 && y < RANKS;
    }

    public Square offset(int dx, int dy) {
        return new Square(x + dx, y + dy);
    }

    int index() {
        return y * FILES + x;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
