package com.janggi.server.record;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.core.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "cho_formation", nullable = false)
    private Formation choFormation;

    @Enumerated(EnumType.STRING)
    @Column(name = "han_formation", nullable = false)
    private Formation hanFormation;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_side")
    private Side aiSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_level")
    private Engine.Level aiLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameResult result;

    @Column(name = "end_reason")
    private String endReason;

    @Column(name = "move_count", nullable = false)
    private int moveCount;

    @Column(name = "cho_user_id")
    private Long choUserId;

    @Column(name = "han_user_id")
    private Long hanUserId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "base_seconds")
    private Integer baseSeconds;

    @Column(name = "increment_seconds")
    private Integer incrementSeconds;

    @Column(name = "cho_ms_left")
    private Long choMsLeft;

    @Column(name = "han_ms_left")
    private Long hanMsLeft;

    @Column(name = "clock_updated_at")
    private OffsetDateTime clockUpdatedAt;

    @Column(name = "cho_rating_change")
    private Integer choRatingChange;

    @Column(name = "han_rating_change")
    private Integer hanRatingChange;

    protected GameRecord() {
    }

    public GameRecord(UUID id, Formation cho, Formation han, Side aiSide, Engine.Level aiLevel,
            Long choUserId, Long hanUserId, OffsetDateTime startedAt) {
        this.id = id;
        this.choUserId = choUserId;
        this.hanUserId = hanUserId;
        this.choFormation = cho;
        this.hanFormation = han;
        this.aiSide = aiSide;
        this.aiLevel = aiLevel;
        this.result = GameResult.ONGOING;
        this.startedAt = startedAt;
    }

    /** 온라인 대국으로 표시하고 두 시계를 기본 시간으로 맞춘다. */
    public void startClock(int baseSeconds, int incrementSeconds, OffsetDateTime at) {
        this.online = true;
        this.baseSeconds = baseSeconds;
        this.incrementSeconds = incrementSeconds;
        clock(baseSeconds * 1000L, baseSeconds * 1000L, at);
    }

    public void moved(int moveCount) {
        this.moveCount = moveCount;
    }

    public void clock(long choMsLeft, long hanMsLeft, OffsetDateTime at) {
        this.choMsLeft = choMsLeft;
        this.hanMsLeft = hanMsLeft;
        this.clockUpdatedAt = at;
    }

    public void ratingChanged(int cho, int han) {
        this.choRatingChange = cho;
        this.hanRatingChange = han;
    }

    public void finish(GameResult result, String endReason, OffsetDateTime endedAt) {
        this.result = result;
        this.endReason = endReason;
        this.endedAt = endedAt;
    }

    public UUID getId() {
        return id;
    }

    public Formation getChoFormation() {
        return choFormation;
    }

    public Formation getHanFormation() {
        return hanFormation;
    }

    public Side getAiSide() {
        return aiSide;
    }

    public Engine.Level getAiLevel() {
        return aiLevel;
    }

    public GameResult getResult() {
        return result;
    }

    public String getEndReason() {
        return endReason;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public Long getChoUserId() {
        return choUserId;
    }

    public Long getHanUserId() {
        return hanUserId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public boolean isOnline() {
        return online;
    }

    public Integer getBaseSeconds() {
        return baseSeconds;
    }

    public Integer getIncrementSeconds() {
        return incrementSeconds;
    }

    public Long getChoMsLeft() {
        return choMsLeft;
    }

    public Long getHanMsLeft() {
        return hanMsLeft;
    }

    public OffsetDateTime getClockUpdatedAt() {
        return clockUpdatedAt;
    }

    public Integer getChoRatingChange() {
        return choRatingChange;
    }

    public Integer getHanRatingChange() {
        return hanRatingChange;
    }
}
