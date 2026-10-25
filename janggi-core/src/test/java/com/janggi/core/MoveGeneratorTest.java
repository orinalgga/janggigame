package com.janggi.core;

import static com.janggi.core.PieceType.*;
import static com.janggi.core.Side.CHO;
import static com.janggi.core.Side.HAN;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MoveGeneratorTest {

    private static Board board(Object... pieces) {
        Board b = Board.empty();
        for (int i = 0; i < pieces.length; i += 4) {
            b.set(Square.of((int) pieces[i + 2], (int) pieces[i + 3]),
                    new Piece((Side) pieces[i], (PieceType) pieces[i + 1]));
        }
        return b;
    }

    private static Set<Square> targets(Board b, int x, int y) {
        return MoveGenerator.pseudoLegalMovesFrom(b, Square.of(x, y)).stream()
                .map(Move::to)
                .collect(Collectors.toSet());
    }

    @Test
    void horseIsBlockedByPieceOnItsLeg() {
        Board open = board(CHO, HORSE, 4, 4);
        assertEquals(8, targets(open, 4, 4).size());

        Board blocked = board(CHO, HORSE, 4, 4, CHO, SOLDIER, 4, 5);
        Set<Square> t = targets(blocked, 4, 4);
        assertEquals(6, t.size());
        assertFalse(t.contains(Square.of(3, 6)));
        assertFalse(t.contains(Square.of(5, 6)));
    }

    @Test
    void elephantIsBlockedOnEitherIntermediateSquare() {
        Board open = board(CHO, ELEPHANT, 4, 4);
        assertEquals(8, targets(open, 4, 4).size());
        assertTrue(targets(open, 4, 4).contains(Square.of(6, 7)));

        Board blockedMid = board(CHO, ELEPHANT, 4, 4, HAN, SOLDIER, 5, 6);
        assertFalse(targets(blockedMid, 4, 4).contains(Square.of(6, 7)));
    }

    @Test
    void cannonNeedsExactlyOneNonCannonScreen() {
        Board noScreen = board(CHO, CANNON, 0, 0);
        assertTrue(targets(noScreen, 0, 0).isEmpty());

        Board screen = board(CHO, CANNON, 0, 0, CHO, SOLDIER, 0, 3, HAN, CHARIOT, 0, 6);
        assertEquals(Set.of(Square.of(0, 4), Square.of(0, 5), Square.of(0, 6)), targets(screen, 0, 0));

        Board cannonScreen = board(CHO, CANNON, 0, 0, HAN, CANNON, 0, 3);
        assertTrue(targets(cannonScreen, 0, 0).isEmpty());
    }

    @Test
    void cannonCannotCaptureCannon() {
        Board b = board(CHO, CANNON, 0, 0, CHO, SOLDIER, 0, 3, HAN, CANNON, 0, 6);
        assertEquals(Set.of(Square.of(0, 4), Square.of(0, 5)), targets(b, 0, 0));
    }

    @Test
    void cannonJumpsAcrossPalaceDiagonal() {
        Board b = board(CHO, CANNON, 3, 7, HAN, GUARD, 4, 8, HAN, CHARIOT, 5, 9);
        assertTrue(targets(b, 3, 7).contains(Square.of(5, 9)));
    }

    @Test
    void chariotSlidesAlongPalaceDiagonal() {
        Board b = board(CHO, CHARIOT, 3, 7);
        Set<Square> t = targets(b, 3, 7);
        assertTrue(t.contains(Square.of(4, 8)));
        assertTrue(t.contains(Square.of(5, 9)));

        Board blocked = board(CHO, CHARIOT, 3, 7, HAN, GUARD, 4, 8);
        Set<Square> tb = targets(blocked, 3, 7);
        assertTrue(tb.contains(Square.of(4, 8)));
        assertFalse(tb.contains(Square.of(5, 9)));
    }

    @Test
    void generalStaysInsideOwnPalace() {
        Board b = board(CHO, GENERAL, 3, 2);
        Set<Square> t = targets(b, 3, 2);
        assertEquals(Set.of(Square.of(4, 2), Square.of(3, 1), Square.of(4, 1)), t);
    }

    @Test
    void generalOnSideMidpointHasNoDiagonal() {
        Board b = board(CHO, GENERAL, 4, 0);
        assertEquals(Set.of(Square.of(3, 0), Square.of(5, 0), Square.of(4, 1)), targets(b, 4, 0));
    }

    @Test
    void soldierMovesForwardAndSidewaysOnly() {
        Board cho = board(CHO, SOLDIER, 4, 4);
        assertEquals(Set.of(Square.of(4, 5), Square.of(3, 4), Square.of(5, 4)), targets(cho, 4, 4));

        Board han = board(HAN, SOLDIER, 4, 5);
        assertEquals(Set.of(Square.of(4, 4), Square.of(3, 5), Square.of(5, 5)), targets(han, 4, 5));
    }

    @Test
    void soldierMovesDiagonallyForwardInEnemyPalace() {
        Board b = board(CHO, SOLDIER, 3, 7);
        assertTrue(targets(b, 3, 7).contains(Square.of(4, 8)));

        Board center = board(CHO, SOLDIER, 4, 8);
        Set<Square> t = targets(center, 4, 8);
        assertTrue(t.contains(Square.of(3, 9)));
        assertTrue(t.contains(Square.of(5, 9)));
        assertFalse(t.contains(Square.of(3, 7)));
    }
}
