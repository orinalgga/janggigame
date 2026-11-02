package com.janggi.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janggi.core.Side;
import org.junit.jupiter.api.Test;

class GameClockTest {

    @Test
    void movingDeductsUsedTimeAndSwitchesSides() {
        GameClock clock = GameClock.fresh(10_000, 3, 30_000);
        clock.start(Side.CHO, 0);
        assertEquals(7_000, clock.remaining(Side.CHO, 3_000));
        assertEquals(10_000, clock.remaining(Side.HAN, 3_000));

        clock.punch(3_000);
        assertEquals(7_000, clock.remaining(Side.CHO, 5_000));
        assertEquals(8_000, clock.remaining(Side.HAN, 5_000));
        assertEquals(Side.HAN, clock.snapshot(5_000).running());
        assertEquals(3, clock.snapshot(5_000).choPeriods());
    }

    @Test
    void byoyomiPeriodIsKeptWhenMovingInTimeAndLostWhenOver() {
        GameClock clock = GameClock.fresh(10_000, 3, 30_000);
        clock.start(Side.CHO, 0);
        // 기본 시간 10초를 다 쓰고 초읽기 20초째.
        GameClock.Snapshot s = clock.snapshot(30_000);
        assertEquals(0, s.choMs());
        assertEquals(3, s.choPeriods());
        assertEquals(10_000, s.periodLeftMs());
        // 30초 안에 두면 횟수는 그대로.
        clock.punch(30_000);
        assertEquals(3, clock.periods(Side.CHO, 30_000));

        clock.punch(31_000); // 한
        // 초가 초읽기 한 번(30초)을 넘겨 45초 만에 두면 한 회가 빠진다.
        assertEquals(3, clock.snapshot(60_999).choPeriods());
        assertEquals(2, clock.snapshot(76_000).choPeriods());
        assertEquals(15_000, clock.snapshot(76_000).periodLeftMs());
        clock.punch(76_000);
        assertEquals(2, clock.periods(Side.CHO, 76_000));
        assertEquals(0, clock.remaining(Side.CHO, 76_000));
    }

    @Test
    void flagsWhenAllByoyomiPeriodsAreUsed() {
        GameClock clock = GameClock.fresh(1_000, 3, 30_000);
        clock.start(Side.CHO, 0);
        assertFalse(clock.flagged(1_000));
        assertFalse(clock.flagged(90_999));
        assertTrue(clock.flagged(91_000));
        assertEquals(0, clock.snapshot(95_000).choPeriods());
    }

    @Test
    void flagsWhenRunningSideRunsOutWithoutByoyomi() {
        GameClock clock = GameClock.fresh(1_000, 0, 0);
        clock.start(Side.CHO, 0);
        assertFalse(clock.flagged(999));
        assertTrue(clock.flagged(1_000));
        assertEquals(0, clock.snapshot(5_000).choMs());

        clock.stop(5_000);
        assertFalse(clock.flagged(9_000));
        assertEquals(null, clock.snapshot(9_000).running());
    }
}
