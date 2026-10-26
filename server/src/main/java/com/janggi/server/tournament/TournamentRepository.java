package com.janggi.server.tournament;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatusIn(List<Tournament.Status> statuses);

    /** 로비 목록: 아직 안 끝났거나 최근에 끝난 대회. */
    List<Tournament> findTop20ByStatusInOrFinishedAtAfterOrderByStartsAtAsc(List<Tournament.Status> statuses,
            OffsetDateTime finishedAfter);

    List<Tournament> findTop50ByOrderByStartsAtDesc();

    boolean existsByHostIdAndStatusIn(long hostId, List<Tournament.Status> statuses);
}
