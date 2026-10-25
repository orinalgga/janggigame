package com.janggi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 한 국면. 불변 객체이고 {@link #apply(Move)}는 새 국면을 돌려준다. */
public final class GameState {

    /** 후수인 한에게 주는 덤. */
    public static final double HAN_BONUS = 1.5;

    private final Board board;
    private final Side sideToMove;

    public GameState(Board board, Side sideToMove) {
        this.board = board;
        this.sideToMove = sideToMove;
    }

    public static GameState initial(Formation cho, Formation han) {
        return new GameState(Board.initial(cho, han), Side.CHO);
    }

    public Board board() {
        return board.copy();
    }

    public Side sideToMove() {
        return sideToMove;
    }

    public Piece pieceAt(Square s) {
        return board.get(s);
    }

    /** 자기 장을 잡히게 두는 수를 뺀 합법 수. 장군이 아니면 한수쉼도 포함한다. */
    public List<Move> legalMoves() {
        List<Move> legal = new ArrayList<>();
        for (Move m : MoveGenerator.pseudoLegalMoves(board, sideToMove)) {
            if (!leavesGeneralAttacked(m)) {
                legal.add(m);
            }
        }
        if (!isCheck()) {
            legal.add(Move.pass());
        }
        return legal;
    }

    public List<Move> legalMovesFrom(Square from) {
        Piece p = board.get(from);
        if (p == null || p.side() != sideToMove) {
            return List.of();
        }
        List<Move> legal = new ArrayList<>();
        for (Move m : MoveGenerator.pseudoLegalMovesFrom(board, from)) {
            if (!leavesGeneralAttacked(m)) {
                legal.add(m);
            }
        }
        return legal;
    }

    public boolean isLegal(Move move) {
        if (move.isPass()) {
            return !isCheck();
        }
        return legalMovesFrom(move.from()).contains(move);
    }

    public GameState apply(Move move) {
        if (!isLegal(move)) {
            throw new IllegalMoveException(move);
        }
        return new GameState(play(board, move), sideToMove.opponent());
    }

    /** 수를 둘 차례인 쪽이 장군을 받고 있는지. */
    public boolean isCheck() {
        return isGeneralAttacked(board, sideToMove);
    }

    /** 장군을 받았는데 피할 수가 없으면 외통. */
    public boolean isCheckmate() {
        return isCheck() && legalMoves().isEmpty();
    }

    /** 두 장이 같은 세로줄에서 사이에 아무것도 없이 마주 보는 상태(빅장). */
    public boolean isBikjang() {
        Optional<Square> cho = board.findGeneral(Side.CHO);
        Optional<Square> han = board.findGeneral(Side.HAN);
        if (cho.isEmpty() || han.isEmpty() || cho.get().x() != han.get().x()) {
            return false;
        }
        int x = cho.get().x();
        for (int y = cho.get().y() + 1; y < han.get().y(); y++) {
            if (!board.isEmpty(Square.of(x, y))) {
                return false;
            }
        }
        return true;
    }

    /** 남은 기물 점수. 한은 덤 1.5점을 더한다. */
    public double score(Side side) {
        double total = side == Side.HAN ? HAN_BONUS : 0;
        for (Square s : board.squaresOf(side)) {
            total += board.get(s).type().points();
        }
        return total;
    }

    private boolean leavesGeneralAttacked(Move m) {
        return isGeneralAttacked(play(board, m), sideToMove);
    }

    private static boolean isGeneralAttacked(Board b, Side side) {
        return b.findGeneral(side)
                .map(g -> MoveGenerator.isAttacked(b, g, side.opponent()))
                .orElse(true);
    }

    private static Board play(Board b, Move m) {
        Board next = b.copy();
        if (!m.isPass()) {
            next.set(m.to(), next.get(m.from()));
            next.set(m.from(), null);
        }
        return next;
    }
}
