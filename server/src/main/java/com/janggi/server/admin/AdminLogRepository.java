package com.janggi.server.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    List<AdminLog> findTop100ByOrderByCreatedAtDescIdDesc();
}
