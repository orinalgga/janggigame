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

    public void moved(int moveCount) {
        this.moveCount = moveCount;
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
}
