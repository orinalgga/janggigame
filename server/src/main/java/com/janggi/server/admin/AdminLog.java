package com.janggi.server.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 관리자 작업 한 건. */
@Entity
@Table(name = "admin_logs")
public class AdminLog {

    public enum Action {
        RENAME, BAN, UNBAN, GRANT, COINS, NOTICE_CREATE, NOTICE_UPDATE, NOTICE_DELETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(name = "target_user_id")
    private Long targetUserId;

    private String detail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AdminLog() {
    }

    public AdminLog(long adminId, Action action, Long targetUserId, String detail, OffsetDateTime createdAt) {
        this.adminId = adminId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.detail = detail == null || detail.length() <= 300 ? detail : detail.substring(0, 300);
        this.createdAt = createdAt;
    }

    public long getAdminId() {
        return adminId;
    }

    public Action getAction() {
        return action;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
