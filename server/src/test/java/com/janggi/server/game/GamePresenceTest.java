package com.janggi.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import org.junit.jupiter.api.Test;

class GamePresenceTest {

    static final long NOTICE = 5_000;
    static final long GRACE = 60_000;

    private String endReason;

    private GameSession online() {
        GameClock clock = GameClock.fresh(600_000, 3, 30_000);
        clock.start(Side.CHO, System.currentTimeMillis());
        GameDto.PlayerDto cho = new GameDto.PlayerDto(1, "초", 1200, "", null);
        GameDto.PlayerDto han = new GameDto.PlayerDto(2, "한", 1200, "", null);
        return new GameSession(GameState.initial(Formation.SANG_MA_SANG_MA, Formation.SANG_MA_SANG_MA), null, null,
                new GameSession.Players(cho, han), clock, 0, null, new GameSession.Listener() {
                    @Override
                    public void onMove(int seq, Move move, GameState after, GameClock.Snapshot c) {
                    }

                    @Override
                    public RatingChange onEnd(GameResult result, String reason) {
                        endReason = reason;
                        return null;
                    }
                });
    }

    @Test
    void shortAbsenceLikeRefreshIsNotShown() {
        GameSession s = online();
        long t0 = System.currentTimeMillis();
        assertFalse(s.updatePresence(true, false, t0 + 3_000, NOTICE, GRACE));
        assertFalse(s.updatePresence(true, true, t0 + 4_000, NOTICE, GRACE));
        assertNull(s.awayMsLeft(Side.HAN, GRACE));
    }

    @Test
    void leaverIsShownAndComingBackClearsIt() {
        GameSession s = online();
        long t0 = System.currentTimeMillis();
        s.updatePresence(true, true, t0, NOTICE, GRACE);
        assertTrue(s.updatePresence(true, false, t0 + 6_000, NOTICE, GRACE), "나감을 알린다");
        assertTrue(s.awayMsLeft(Side.HAN, GRACE) > 0);
        assertNull(s.awayMsLeft(Side.CHO, GRACE));
        assertFalse(s.updatePresence(true, false, t0 + 30_000, NOTICE, GRACE), "이미 알렸으면 다시 안 알린다");

        assertTrue(s.updatePresence(true, true, t0 + 40_000, NOTICE, GRACE), "돌아왔음을 알린다");
        assertNull(s.awayMsLeft(Side.HAN, GRACE));
        // 돌아온 뒤에는 처음부터 다시 센다.
        assertFalse(s.updatePresence(true, false, t0 + 90_000, NOTICE, GRACE) && s.isOver());
        assertFalse(s.isOver());
    }

    @Test
    void leaverWhoDoesNotComeBackLoses() {
        GameSession s = online();
        long t0 = System.currentTimeMillis();
        s.updatePresence(true, true, t0, NOTICE, GRACE);
        s.updatePresence(true, false, t0 + 10_000, NOTICE, GRACE);
        assertFalse(s.isOver());
        assertTrue(s.updatePresence(true, false, t0 + 60_000, NOTICE, GRACE));
        assertTrue(s.isOver());
        assertEquals(GameResult.CHO_WIN, s.result());
        assertEquals("DISCONNECT", s.endReason());
        assertEquals("DISCONNECT", endReason);
        assertNull(s.awayMsLeft(Side.HAN, GRACE));
        assertFalse(s.updatePresence(true, false, t0 + 70_000, NOTICE, GRACE), "끝난 대국은 그대로");
    }

    @Test
    void whenBothLeaveNobodyWinsByDisconnect() {
        GameSession s = online();
        long t0 = System.currentTimeMillis();
        s.updatePresence(false, false, t0 + 120_000, NOTICE, GRACE);
        assertFalse(s.isOver(), "둘 다 나갔으면 시계에 맡긴다");
    }
}
