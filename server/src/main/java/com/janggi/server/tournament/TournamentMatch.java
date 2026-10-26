package com.janggi.server.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 대진표 한 칸. 한쪽이 비어 있으면 부전승이다. */
@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private long tournamentId;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private int slot;

    @Column(name = "cho_user_id")
    private Long choUserId;

    @Column(name = "han_user_id")
    private Long hanUserId;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TournamentMatch() {
    }

    TournamentMatch(long tournamentId, int round, int slot, Long choUserId, Long hanUserId, OffsetDateTime now) {
        this.tournamentId = tournamentId;
        this.round = round;
        this.slot = slot;
        this.choUserId = choUserId;
        this.hanUserId = hanUserId;
        this.createdAt = now;
    }

    void game(UUID gameId) {
        this.gameId = gameId;
    }

    void won(long userId) {
        this.winnerId = userId;
    }

    boolean bye() {
        return choUserId == null || hanUserId == null;
    }

    public int getRound() {
        return round;
    }

    public int getSlot() {
        return slot;
    }

    public Long getChoUserId() {
        return choUserId;
    }

    public Long getHanUserId() {
        return hanUserId;
    }

    public UUID getGameId() {
        return gameId;
    }

    public Long getWinnerId() {
        return winnerId;
    }
}
