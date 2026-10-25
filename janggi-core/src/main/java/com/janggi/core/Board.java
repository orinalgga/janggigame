package com.janggi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 기물 배치만 담는 판. 규칙은 {@link MoveGenerator}와 {@link GameState}에 있다. */
public final class Board {

    private static final int[] FORMATION_FILES = {1, 2, 6, 7};

    private final Piece[] cells;

    private Board(Piece[] cells) {
        this.cells = cells;
    }

    public static Board empty() {
        return new Board(new Piece[Square.FILES * Square.RANKS]);
    }

    public static Board initial(Formation cho, Formation han) {
        Board b = empty();
        b.placeSide(Side.CHO, cho);
        b.placeSide(Side.HAN, han);
        return b;
    }

    private void placeSide(Side side, Formation formation) {
        put(side, PieceType.CHARIOT, 0, 0);
        put(side, PieceType.CHARIOT, 8, 0);
        put(side, PieceType.GUARD, 3, 0);
        put(side, PieceType.GUARD, 5, 0);
        put(side, PieceType.GENERAL, 4, 1);
        put(side, PieceType.CANNON, 1, 2);
        put(side, PieceType.CANNON, 7, 2);
        for (int x = 0; x <= 8; x += 2) {
            put(side, PieceType.SOLDIER, x, 3);
        }
        for (int i = 0; i < 4; i++) {
            // 한은 판 반대편에 앉으므로 자기 왼쪽이 x=7 쪽이다.
            int x = side == Side.CHO ? FORMATION_FILES[i] : 8 - FORMATION_FILES[i];
            put(side, formation.at(i), x, 0);
        }
    }

    /** 초 기준 좌표(x, y)를 받아 한이면 위아래를 뒤집어 놓는다. */
    private void put(Side side, PieceType type, int x, int y) {
        int realY = side == Side.CHO ? y : Square.RANKS - 1 - y;
        set(Square.of(x, realY), new Piece(side, type));
    }

    public Piece get(Square s) {
        return cells[s.index()];
    }

    public boolean isEmpty(Square s) {
        return get(s) == null;
    }

    public void set(Square s, Piece p) {
        cells[s.index()] = p;
    }

    public Board copy() {
        return new Board(cells.clone());
    }

    public Optional<Square> findGeneral(Side side) {
        for (int i = 0; i < cells.length; i++) {
            Piece p = cells[i];
            if (p != null && p.side() == side && p.is(PieceType.GENERAL)) {
                return Optional.of(Square.of(i % Square.FILES, i / Square.FILES));
            }
        }
        return Optional.empty();
    }

    public List<Square> squaresOf(Side side) {
        List<Square> result = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != null && cells[i].side() == side) {
                result.add(Square.of(i % Square.FILES, i / Square.FILES));
            }
        }
        return result;
    }
}
