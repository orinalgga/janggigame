package com.janggi.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AttacksTest {

    /** 무작위로 둔 국면마다 빠른 공격 판정이 상대 수를 모두 만들어 보는 방식과 같은지 확인한다. */
    @Test
    void matchesBruteForceOnRandomGames() {
        Random random = new Random(42);
        Formation[] formations = Formation.values();
        int checked = 0;
        for (int game = 0; game < 200; game++) {
            GameState s = GameState.initial(
                    formations[random.nextInt(formations.length)], formations[random.nextInt(formations.length)]);
            for (int ply = 0; ply < 120 && !s.isGameOver(); ply++) {
                for (Side side : Side.values()) {
                    Board b = s.board();
                    Square general = b.findGeneral(side).orElseThrow();
                    assertEquals(
                            MoveGenerator.isAttacked(b, general, side.opponent()),
                            Attacks.onGeneral(b, general, side.opponent()),
                            () -> "game " + " side " + side);
                    checked++;
                }
                List<Move> moves = s.legalMoves();
                s = s.apply(moves.get(random.nextInt(moves.size())));
            }
        }
        System.out.println("attack checks: " + checked);
    }
}
