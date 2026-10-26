package com.janggi.server.tournament;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    List<TournamentMatch> findByTournamentIdOrderByRoundAscSlotAsc(long tournamentId);

    List<TournamentMatch> findByTournamentIdAndRoundOrderBySlotAsc(long tournamentId, int round);
}
