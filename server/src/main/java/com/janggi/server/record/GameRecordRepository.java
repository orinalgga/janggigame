package com.janggi.server.record;

import com.janggi.core.GameResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecord, UUID> {

    List<GameRecord> findTop20ByOrderByStartedAtDesc();

    List<GameRecord> findTop20ByChoUserIdOrHanUserIdOrderByStartedAtDesc(Long choUserId, Long hanUserId);

    List<GameRecord> findByOnlineTrueAndResult(GameResult result);
}
