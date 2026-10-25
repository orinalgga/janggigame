package com.janggi.core;

public record Piece(Side side, PieceType type) {

    public boolean is(PieceType t) {
        return type == t;
    }
}
