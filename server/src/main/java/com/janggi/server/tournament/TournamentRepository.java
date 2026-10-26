package com.janggi.server.tournament;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatusIn(List<Tournament.Status> statuses);

    /** 진행·취소처럼 상태를 바꿀 때는 줄을 잠가 두 곳에서 동시에 끝내지(상금을 두 번 주지) 않게 한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tournament t where t.id = :id")
    Optional<Tournament> findForUpdate(long id);

    /** 로비 목록: 아직 안 끝났거나 최근에 끝난 대회. */
    List<Tournament> findTop20ByStatusInOrFinishedAtAfterOrderByStartsAtAsc(List<Tournament.Status> statuses,
            OffsetDateTime finishedAfter);

    List<Tournament> findTop50ByOrderByStartsAtDesc();

    boolean existsByHostIdAndStatusIn(long hostId, List<Tournament.Status> statuses);
}
