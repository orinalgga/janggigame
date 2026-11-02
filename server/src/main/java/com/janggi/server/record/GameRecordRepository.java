package com.janggi.server.record;

import com.janggi.core.GameResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRecordRepository extends JpaRepository<GameRecord, UUID> {

    List<GameRecord> findTop20ByOrderByStartedAtDesc();

    List<GameRecord> findTop20ByChoUserIdOrHanUserIdOrderByStartedAtDesc(Long choUserId, Long hanUserId);

    List<GameRecord> findByOnlineTrueAndResult(GameResult result);

    List<GameRecord> findTop10ByOnlineTrueAndChoUserIdOrOnlineTrueAndHanUserIdOrderByStartedAtDesc(Long choUserId,
            Long hanUserId);

    /** 온라인 대국에서 이 회원 쪽 결과별 판 수. [이김, 짐, 비김] 한 줄로 돌려준다. */
    @Query("""
            select
              coalesce(sum(case when (g.choUserId = :id and g.result = com.janggi.core.GameResult.CHO_WIN)
                                  or (g.hanUserId = :id and g.result = com.janggi.core.GameResult.HAN_WIN)
                                then 1 else 0 end), 0),
              coalesce(sum(case when (g.choUserId = :id and g.result = com.janggi.core.GameResult.HAN_WIN)
                                  or (g.hanUserId = :id and g.result = com.janggi.core.GameResult.CHO_WIN)
                                then 1 else 0 end), 0),
              coalesce(sum(case when g.result = com.janggi.core.GameResult.DRAW then 1 else 0 end), 0)
            from GameRecord g
            where g.online = true and (g.choUserId = :id or g.hanUserId = :id)
            """)
    List<Object[]> onlineRecordOf(@Param("id") long userId);

    long countByStartedAtGreaterThanEqual(OffsetDateTime from);

    long countByOnlineTrueAndResult(GameResult result);
}
