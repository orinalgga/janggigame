package com.janggi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 한 국면. 불변 객체이고 {@link #apply(Move)}는 새 국면을 돌려준다. */
public final class GameState {

    /** 후수인 한에게 주는 덤. */
    public static final double HAN_BONUS = 1.5;

    /** 빅장이 이만큼 이어지면(빅장이 된 수부터 센 수) 점수로 판정해 끝낸다. */
    public static final int BIKJANG_LIMIT = 5;

    private final Board board;
    private final Side sideToMove;
    private final int bikjangCount;

    public GameState(Board board, Side sideToMove) {
        this(board, sideToMove, 0);
    }

    /** bikjangCount는 빅장을 풀지 않고 이어 둔 수. 빅장이 아니면 0. */
    public GameState(Board board, Side sideToMove, int bikjangCount) {
        this.board = board;
        this.sideToMove = sideToMove;
        this.bikjangCount = bikjangCount;
    }

    public static GameState initial(Formation cho, Formation han) {
        return new GameState(Board.initial(cho, han), Side.CHO);
    }

    public Board board() {
        return board.copy();
    }

    /** 복사하지 않은 판. 엔진 안에서 읽기만 할 때 쓴다. */
    Board boardView() {
        return board;
    }

    public Side sideToMove() {
        return sideToMove;
    }

    public Piece pieceAt(Square s) {
        return board.get(s);
    }

    /** 자기 장을 잡히게 두는 수를 뺀 합법 수. 장군이 아니면 한수쉼도 포함한다. 대국이 끝났으면 비어 있다. */
    public List<Move> legalMoves() {
        return isGameOver() ? List.of() : candidateMoves();
    }

    /** 끝났는지 따지지 않은 합법 수. 엔진이 탐색 중에 쓴다. */
    List<Move> candidateMoves() {
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
        if (p == null || p.side() != sideToMove || isGameOver()) {
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
            return !isCheck() && !isGameOver();
        }
        return legalMovesFrom(move.from()).contains(move);
    }

    public GameState apply(Move move) {
        if (!isLegal(move)) {
            throw new IllegalMoveException(move);
        }
        return next(move);
    }

    /** 합법성 검사 없이 수를 둔다. 엔진이 이미 합법 수만 넘길 때 쓴다. */
    GameState next(Move move) {
        GameState after = new GameState(play(board, move), sideToMove.opponent());
        return after.isBikjang() ? new GameState(after.board, after.sideToMove, bikjangCount + 1) : after;
    }

    /** 수를 둘 차례인 쪽이 장군을 받고 있는지. */
    public boolean isCheck() {
        return isGeneralAttacked(board, sideToMove);
    }

    /** 장군을 받았는데 피할 수가 없으면 외통. */
    public boolean isCheckmate() {
        return isCheck() && candidateMoves().isEmpty();
    }

    /**
     * 외통이면 둔 쪽이 이기고, 빅장이 {@link #BIKJANG_LIMIT}수 이어지면 점수가 높은 쪽이 이긴다.
     * 그 전에 빅장이 풀리면 대국은 이어진다. 둘이 겹치면 외통을 먼저 본다.
     * 양쪽 모두 장과 사만 남으면 더 둘 수가 없으므로 바로 점수로 가린다.
     * 한의 덤 1.5점 때문에 점수가 같을 수 없지만, 같으면 무승부로 둔다.
     */
    public GameResult result() {
        if (isCheckmate()) {
            return sideToMove == Side.CHO ? GameResult.HAN_WIN : GameResult.CHO_WIN;
        }
        if (isScoreDecided()) {
            int cmp = Double.compare(score(Side.CHO), score(Side.HAN));
            return cmp > 0 ? GameResult.CHO_WIN : cmp < 0 ? GameResult.HAN_WIN : GameResult.DRAW;
        }
        return GameResult.ONGOING;
    }

    public boolean isGameOver() {
        return result() != GameResult.ONGOING;
    }

    /** 빅장을 풀지 않고 이어 둔 수. 빅장이 된 수가 1이고, 빅장이 아니면 0. */
    public int bikjangCount() {
        return bikjangCount;
    }

    /** 빅장이 끝까지 이어져 점수 판정으로 끝나는 국면인지. */
    public boolean isBikjangOver() {
        return bikjangCount >= BIKJANG_LIMIT && isBikjang();
    }

    /** 외통 말고 점수로 끝나는 국면인지: 빅장이 끝까지 이어졌거나, 양쪽 모두 공격할 알이 없다. */
    public boolean isScoreDecided() {
        return isBikjangOver() || isOnlyGuardsLeft();
    }

    /**
     * 양쪽 모두 장과 사만 남았는지. 사는 궁성 밖으로 못 나가므로 누구도 외통을 만들 수 없다.
     * 차·포·마·상·병(졸)이 하나라도 있으면 아직 대국이 이어진다.
     */
    public boolean isOnlyGuardsLeft() {
        for (Side side : Side.values()) {
            for (Square s : board.squaresOf(side)) {
                PieceType t = board.get(s).type();
                if (t != PieceType.GENERAL && t != PieceType.GUARD) {
                    return false;
                }
            }
        }
        return true;
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
                .map(g -> Attacks.onGeneral(b, g, side.opponent()))
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
