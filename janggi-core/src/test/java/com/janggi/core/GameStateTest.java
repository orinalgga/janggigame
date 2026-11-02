package com.janggi.core;

import static com.janggi.core.PieceType.*;
import static com.janggi.core.Side.CHO;
import static com.janggi.core.Side.HAN;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameStateTest {

    private static GameState state(Side toMove, Object... pieces) {
        Board b = Board.empty();
        for (int i = 0; i < pieces.length; i += 4) {
            b.set(Square.of((int) pieces[i + 2], (int) pieces[i + 3]),
                    new Piece((Side) pieces[i], (PieceType) pieces[i + 1]));
        }
        return new GameState(b, toMove);
    }

    @Test
    void initialPositionMatchesFormation() {
        GameState s = GameState.initial(Formation.MA_SANG_MA_SANG, Formation.SANG_MA_SANG_MA);
        assertEquals(new Piece(CHO, HORSE), s.pieceAt(Square.of(1, 0)));
        assertEquals(new Piece(CHO, ELEPHANT), s.pieceAt(Square.of(2, 0)));
        assertEquals(new Piece(CHO, GENERAL), s.pieceAt(Square.of(4, 1)));
        // 한은 자기 왼쪽이 x=7이므로 상마상마가 x=7,6,2,1 순서로 놓인다.
        assertEquals(new Piece(HAN, ELEPHANT), s.pieceAt(Square.of(7, 9)));
        assertEquals(new Piece(HAN, HORSE), s.pieceAt(Square.of(6, 9)));
        assertEquals(new Piece(HAN, GENERAL), s.pieceAt(Square.of(4, 8)));
        assertEquals(new Piece(HAN, SOLDIER), s.pieceAt(Square.of(0, 6)));
        assertEquals(CHO, s.sideToMove());
    }

    @Test
    void initialScoresAre72And73Point5() {
        GameState s = GameState.initial(Formation.SANG_MA_MA_SANG, Formation.MA_SANG_SANG_MA);
        assertEquals(72.0, s.score(CHO));
        assertEquals(73.5, s.score(HAN));
    }

    @Test
    void initialPositionHasMovesAndPass() {
        GameState s = GameState.initial(Formation.MA_SANG_MA_SANG, Formation.MA_SANG_MA_SANG);
        assertFalse(s.isCheck());
        assertTrue(s.legalMoves().contains(Move.pass()));
        assertTrue(s.isLegal(Move.of(0, 3, 1, 3)));
        assertFalse(s.isLegal(Move.of(0, 3, 0, 2)));
    }

    @Test
    void cannotMakeMoveThatExposesGeneral() {
        // 초 사가 한 차의 길을 막고 있다. 사가 비키면 장이 잡힌다.
        GameState s = state(CHO,
                CHO, GENERAL, 4, 0,
                CHO, GUARD, 4, 1,
                HAN, CHARIOT, 4, 5,
                HAN, GENERAL, 3, 9);
        assertFalse(s.isLegal(Move.of(4, 1, 3, 1)));
        assertThrows(IllegalMoveException.class, () -> s.apply(Move.of(4, 1, 3, 1)));
    }

    @Test
    void passIsIllegalWhenInCheck() {
        GameState s = state(CHO,
                CHO, GENERAL, 4, 1,
                HAN, CHARIOT, 4, 5,
                HAN, GENERAL, 3, 9);
        assertTrue(s.isCheck());
        assertFalse(s.legalMoves().contains(Move.pass()));
    }

    @Test
    void detectsCheckmate() {
        // 한 장이 궁성 귀퉁이에 몰렸고, 초 차 둘이 두 줄을 모두 막는다.
        GameState s = state(HAN,
                HAN, GENERAL, 3, 9,
                CHO, CHARIOT, 0, 9,
                CHO, CHARIOT, 0, 8,
                CHO, GENERAL, 5, 0);
        assertTrue(s.isCheck());
        assertTrue(s.isCheckmate());
    }

    @Test
    void checkThatCanBeEscapedIsNotMate() {
        GameState s = state(HAN,
                HAN, GENERAL, 3, 9,
                HAN, GUARD, 5, 8,
                CHO, CHARIOT, 0, 9,
                CHO, GENERAL, 5, 0);
        assertTrue(s.isCheck());
        assertFalse(s.isCheckmate());
    }

    @Test
    void detectsBikjang() {
        GameState facing = state(CHO, CHO, GENERAL, 4, 1, HAN, GENERAL, 4, 8);
        assertTrue(facing.isBikjang());

        GameState blocked = state(CHO, CHO, GENERAL, 4, 1, HAN, GENERAL, 4, 8, CHO, SOLDIER, 4, 4);
        assertFalse(blocked.isBikjang());
    }

    @Test
    void bikjangIsDecidedByScoreOnlyAfterFiveMovesInARow() {
        // 초는 장 + 차(13), 한은 장 + 덤(1.5) → 빅장이 5수 이어지면 초가 이긴다.
        GameState s = state(HAN, CHO, GENERAL, 4, 1, HAN, GENERAL, 3, 8, CHO, CHARIOT, 0, 4);
        assertEquals(GameResult.ONGOING, s.result());

        GameState next = s.apply(Move.of(3, 8, 4, 8));
        assertTrue(next.isBikjang());
        assertEquals(1, next.bikjangCount());
        assertEquals(GameResult.ONGOING, next.result());

        // 빅장을 풀지 않고 네 수를 더 두면(쉬거나 같은 줄에서만 움직이면) 다섯 번째에 끝난다.
        next = next.apply(Move.of(0, 4, 0, 5));
        next = next.apply(Move.pass());
        next = next.apply(Move.of(0, 5, 0, 4));
        assertEquals(4, next.bikjangCount());
        assertEquals(GameResult.ONGOING, next.result());
        GameState last = next.apply(Move.pass());
        assertEquals(5, last.bikjangCount());
        assertEquals(GameResult.CHO_WIN, last.result());
        assertTrue(last.isGameOver());
        assertTrue(last.legalMoves().isEmpty());
        assertThrows(IllegalMoveException.class, () -> last.apply(Move.pass()));
    }

    @Test
    void breakingBikjangResetsTheCount() {
        GameState s = state(HAN, CHO, GENERAL, 4, 1, HAN, GENERAL, 3, 8, CHO, CHARIOT, 0, 4);
        GameState next = s.apply(Move.of(3, 8, 4, 8)).apply(Move.pass()).apply(Move.pass());
        assertEquals(3, next.bikjangCount());

        // 차가 두 장 사이에 들어서면 빅장이 풀린다.
        next = next.apply(Move.of(0, 4, 4, 4));
        assertFalse(next.isBikjang());
        assertEquals(0, next.bikjangCount());

        // 장군을 피한 뒤 다시 빅장이 되면 1부터 센다.
        next = next.apply(Move.of(4, 8, 3, 8)).apply(Move.of(4, 4, 0, 4)).apply(Move.of(3, 8, 4, 8));
        assertEquals(1, next.bikjangCount());
        assertEquals(GameResult.ONGOING, next.result());
    }

    @Test
    void bikjangGoesToHanWhenScoresAreEqualBeforeBonus() {
        // 양쪽 모두 장 + 사(3)라 기물 점수가 같고, 덤 1.5점으로 한이 이긴다.
        GameState s = state(CHO, CHO, GENERAL, 3, 1, CHO, GUARD, 5, 0, HAN, GENERAL, 4, 8, HAN, GUARD, 5, 9);
        GameState next = s.apply(Move.of(3, 1, 4, 1));
        for (int i = 1; i < GameState.BIKJANG_LIMIT; i++) {
            next = next.apply(Move.pass());
        }
        assertEquals(GameResult.HAN_WIN, next.result());
    }

    @Test
    void checkmateGivesWinToOtherSide() {
        GameState s = state(HAN,
                HAN, GENERAL, 3, 9,
                CHO, CHARIOT, 0, 9,
                CHO, CHARIOT, 0, 8,
                CHO, GENERAL, 5, 0);
        assertEquals(GameResult.CHO_WIN, s.result());
        assertTrue(s.legalMoves().isEmpty());
    }

    @Test
    void applySwitchesTurnAndMovesPiece() {
        GameState s = GameState.initial(Formation.MA_SANG_MA_SANG, Formation.MA_SANG_MA_SANG);
        GameState next = s.apply(Move.of(0, 3, 1, 3));
        assertEquals(HAN, next.sideToMove());
        assertNull(next.pieceAt(Square.of(0, 3)));
        assertEquals(new Piece(CHO, SOLDIER), next.pieceAt(Square.of(1, 3)));
        assertEquals(new Piece(CHO, SOLDIER), s.pieceAt(Square.of(0, 3)));
    }
}
