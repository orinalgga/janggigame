package com.janggi.core;

import static com.janggi.core.PieceType.*;
import static com.janggi.core.Side.CHO;
import static com.janggi.core.Side.HAN;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class EngineTest {

    private static GameState state(Side toMove, Object... pieces) {
        Board b = Board.empty();
        for (int i = 0; i < pieces.length; i += 4) {
            b.set(Square.of((int) pieces[i + 2], (int) pieces[i + 3]),
                    new Piece((Side) pieces[i], (PieceType) pieces[i + 1]));
        }
        return new GameState(b, toMove);
    }

    @Test
    void doesNotPassWhenAMoveIsJustAsGood() {
        // 초급은 비슷한 수 중 무작위로 고르는데, 그 안에 한수쉼이 섞여 첫 수부터 쉬는 일이 있었다.
        GameState s = GameState.initial(Formation.SANG_MA_SANG_MA, Formation.SANG_MA_SANG_MA);
        for (int seed = 0; seed < 300; seed++) {
            assertFalse(new Engine(new Random(seed)).bestMove(s, Engine.Level.BEGINNER).isPass(), "seed " + seed);
        }
    }

    @Test
    void findsMateInOne() {
        GameState s = state(CHO,
                HAN, GENERAL, 3, 9,
                CHO, CHARIOT, 0, 8,
                CHO, CHARIOT, 1, 1,
                CHO, GENERAL, 5, 0);
        Move m = new Engine(new Random(1)).bestMove(s, Engine.Level.ADVANCED);
        assertEquals(Move.of(1, 1, 1, 9), m);
        assertTrue(s.apply(m).isCheckmate());
    }

    @Test
    void capturesHangingChariot() {
        GameState s = state(CHO,
                CHO, GENERAL, 4, 1,
                CHO, CHARIOT, 0, 0,
                HAN, GENERAL, 3, 8,
                HAN, CHARIOT, 0, 6);
        assertEquals(Move.of(0, 0, 0, 6), new Engine(new Random(1)).bestMove(s, Engine.Level.INTERMEDIATE));
    }

    @Test
    void everyLevelReturnsLegalMoveInTime() {
        GameState s = GameState.initial(Formation.MA_SANG_MA_SANG, Formation.SANG_MA_SANG_MA);
        for (Engine.Level level : Engine.Level.values()) {
            Engine engine = new Engine(new Random(7));
            long start = System.currentTimeMillis();
            Move m = engine.bestMove(s, level);
            long took = System.currentTimeMillis() - start;
            assertTrue(s.isLegal(m), level + " " + m);
            assertTrue(took < level.timeMillis + 1500, level + " took " + took + "ms");
            System.out.println(level + ": " + m + ", " + engine.nodes() + " nodes, " + took + "ms");
        }
    }

    @Test
    void intermediateBeatsBeginner() {
        Random random = new Random(3);
        int strongWins = 0;
        int games = 4;
        for (int g = 0; g < games; g++) {
            Side strongSide = g % 2 == 0 ? CHO : HAN;
            GameState s = GameState.initial(Formation.MA_SANG_MA_SANG, Formation.MA_SANG_MA_SANG);
            Engine strong = new Engine(random);
            Engine weak = new Engine(random);
            for (int ply = 0; ply < 160 && !s.isGameOver(); ply++) {
                boolean strongTurn = s.sideToMove() == strongSide;
                Move m = strongTurn
                        ? strong.bestMove(s, Engine.Level.INTERMEDIATE)
                        : weak.bestMove(s, Engine.Level.BEGINNER);
                s = s.apply(m);
            }
            GameResult r = s.result();
            boolean strongWon = (r == GameResult.CHO_WIN && strongSide == CHO)
                    || (r == GameResult.HAN_WIN && strongSide == HAN)
                    || (r == GameResult.ONGOING && s.score(strongSide) > s.score(strongSide.opponent()));
            if (strongWon) {
                strongWins++;
            }
            System.out.println("game " + g + ": " + r + ", cho " + s.score(CHO) + " han " + s.score(HAN));
        }
        assertTrue(strongWins >= 3, "intermediate won " + strongWins + "/" + games);
    }
}
