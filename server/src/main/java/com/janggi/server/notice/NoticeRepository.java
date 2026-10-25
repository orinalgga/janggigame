package com.janggi.server.notice;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("select n from Notice n where n.active = true and n.startsAt <= :now"
            + " and (n.endsAt is null or n.endsAt > :now) order by n.startsAt desc, n.id desc")
    List<Notice> showing(OffsetDateTime now);

    List<Notice> findTop100ByOrderByStartsAtDescIdDesc();
}
