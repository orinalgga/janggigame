package com.janggi.server.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tournament_entries")
public class TournamentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private long tournamentId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    private Integer seed;

    @Column(name = "eliminated_round")
    private Integer eliminatedRound;

    @Column(name = "fee_paid", nullable = false)
    private int feePaid;

    @Column(name = "fee_free", nullable = false)
    private int feeFree;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    protected TournamentEntry() {
    }

    TournamentEntry(long tournamentId, long userId, int feePaid, int feeFree, OffsetDateTime joinedAt) {
        this.tournamentId = tournamentId;
        this.userId = userId;
        this.feePaid = feePaid;
        this.feeFree = feeFree;
        this.joinedAt = joinedAt;
    }

    void seed(int seed) {
        this.seed = seed;
    }

    void eliminated(int round) {
        this.eliminatedRound = round;
    }

    public long getUserId() {
        return userId;
    }

    public long getTournamentId() {
        return tournamentId;
    }

    public Integer getSeed() {
        return seed;
    }

    public Integer getEliminatedRound() {
        return eliminatedRound;
    }

    public int getFeePaid() {
        return feePaid;
    }

    public int getFeeFree() {
        return feeFree;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
