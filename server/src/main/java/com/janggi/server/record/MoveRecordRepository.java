package com.janggi.server.record;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveRecordRepository extends JpaRepository<MoveRecord, MoveRecord.Key> {

    List<MoveRecord> findByGameIdOrderBySeq(UUID gameId);
}
