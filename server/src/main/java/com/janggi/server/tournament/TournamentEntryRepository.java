package com.janggi.server.tournament;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {

    List<TournamentEntry> findByTournamentIdOrderByJoinedAtAscIdAsc(long tournamentId);

    Optional<TournamentEntry> findByTournamentIdAndUserId(long tournamentId, long userId);

    long countByTournamentId(long tournamentId);
}
