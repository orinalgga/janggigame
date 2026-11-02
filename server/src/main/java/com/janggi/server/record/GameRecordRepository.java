package com.janggi.server.record;

import com.janggi.core.GameResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRecordRepository extends JpaRepository<GameRecord, UUID> {

    List<GameRecord> findTop20ByOrderByStartedAtDesc();

    List<GameRecord> findTop20ByChoUserIdOrHanUserIdOrderByStartedAtDesc(Long choUserId, Long hanUserId);

    List<GameRecord> findByOnlineTrueAndResult(GameResult result);

    long countByStartedAtGreaterThanEqual(OffsetDateTime from);

    long countByOnlineTrueAndResult(GameResult result);

    /** 관리자 기보 검색: 기간 [from, to) 안에 시작한 대국. anyone이 false면 ids 회원이 둔 대국만. */
    @Query("""
            select g from GameRecord g
            where (:anyone = true or g.choUserId in :ids or g.hanUserId in :ids)
              and g.startedAt >= :from and g.startedAt < :to
            """)
    Page<GameRecord> adminSearch(@Param("anyone") boolean anyone, @Param("ids") List<Long> ids,
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);
}
