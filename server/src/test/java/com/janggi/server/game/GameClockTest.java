package com.janggi.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janggi.core.Side;
import org.junit.jupiter.api.Test;

class GameClockTest {

    @Test
    void movingDeductsUsedTimeAddsIncrementAndSwitchesSides() {
        GameClock clock = new GameClock(10_000, 10_000, 2_000);
        clock.start(Side.CHO, 0);
        assertEquals(7_000, clock.remaining(Side.CHO, 3_000));
        assertEquals(10_000, clock.remaining(Side.HAN, 3_000));

        clock.punch(3_000);
        assertEquals(9_000, clock.remaining(Side.CHO, 5_000));
        assertEquals(8_000, clock.remaining(Side.HAN, 5_000));
        assertEquals(Side.HAN, clock.snapshot(5_000).running());
    }

    @Test
    void flagsWhenRunningSideRunsOut() {
        GameClock clock = new GameClock(1_000, 1_000, 0);
        clock.start(Side.CHO, 0);
        assertFalse(clock.flagged(999));
        assertTrue(clock.flagged(1_000));
        assertEquals(0, clock.snapshot(5_000).choMs());

        clock.stop(5_000);
        assertFalse(clock.flagged(9_000));
        assertEquals(null, clock.snapshot(9_000).running());
    }
}
