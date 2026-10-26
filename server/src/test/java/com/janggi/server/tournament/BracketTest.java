package com.janggi.server.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BracketTest {

    @Test
    void sizeAndRounds() {
        assertThat(Bracket.size(2)).isEqualTo(2);
        assertThat(Bracket.size(5)).isEqualTo(8);
        assertThat(Bracket.size(16)).isEqualTo(16);
        assertThat(Bracket.rounds(16)).isEqualTo(4);
    }

    @Test
    void topSeedsMeetLate() {
        assertThat(Bracket.order(4)).isEqualTo(List.of(1, 4, 2, 3));
        assertThat(Bracket.order(8)).isEqualTo(List.of(1, 8, 4, 5, 2, 7, 3, 6));
    }

    @Test
    void rankByRoundLost() {
        // 16강(4라운드): 결승 패배 2위, 4강 3위, 8강 5위, 16강 9위.
        assertThat(TournamentService.rank(4, 4)).isEqualTo(2);
        assertThat(TournamentService.rank(4, 3)).isEqualTo(3);
        assertThat(TournamentService.rank(4, 2)).isEqualTo(5);
        assertThat(TournamentService.rank(4, 1)).isEqualTo(9);
    }
}
