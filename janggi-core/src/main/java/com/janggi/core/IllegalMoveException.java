package com.janggi.core;

public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException(Move move) {
        super("둘 수 없는 수입니다: " + move);
    }
}
