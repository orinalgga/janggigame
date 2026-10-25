package com.janggi.server.notice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 공지 한 건. 본문은 글자 그대로 보여 준다(HTML 아님). */
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Notice() {
    }

    public Notice(long createdBy, OffsetDateTime now) {
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public void edit(String title, String body, OffsetDateTime startsAt, OffsetDateTime endsAt, boolean active,
            OffsetDateTime now) {
        this.title = title;
        this.body = body;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
