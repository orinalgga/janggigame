package com.janggi.server.record;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecord, UUID> {

    List<GameRecord> findTop20ByOrderByStartedAtDesc();
}
