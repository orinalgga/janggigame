package com.janggi.server.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 대회 하나. 상태는 SCHEDULED(신청 받는 중) → RUNNING → FINISHED, 또는 CANCELLED. */
@Entity
@Table(name = "tournaments")
public class Tournament {

    public enum Status {
        SCHEDULED, RUNNING, FINISHED, CANCELLED
    }

    /** 리그: 누구나, 유단자(초단 이상), 급. 참가 신청할 때의 레이팅으로 본다. */
    public enum Division {
        ALL, DAN, KYU
    }

    /** 정기 리그는 repeatDays마다 다음 회차가 저절로 생긴다. */
    public enum Kind {
        REGULAR, SPECIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "base_seconds", nullable = false)
    private int baseSeconds;

    @Column(name = "increment_seconds", nullable = false)
    private int incrementSeconds;

    @Column(name = "entry_fee", nullable = false)
    private int entryFee;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Division division = Division.ALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind = Kind.SPECIAL;

    @Column(name = "repeat_days", nullable = false)
    private int repeatDays;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "bonus_prize", nullable = false)
    private int bonusPrize;

    @Column(name = "bonus_paid", nullable = false)
    private int bonusPaid;

    @Column(name = "bonus_free", nullable = false)
    private int bonusFree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "current_round", nullable = false)
    private int currentRound;

    @Column(name = "next_round_at")
    private OffsetDateTime nextRoundAt;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(nullable = false)
    private int prize;

    /** 운영 대회 우승자에게 주는 보상 아바타. */
    @Column(name = "prize_item")
    private String prizeItem;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    protected Tournament() {
    }

    public Tournament(long createdBy, OffsetDateTime now) {
        this.createdBy = createdBy;
        this.createdAt = now;
        this.status = Status.SCHEDULED;
    }

    void edit(String title, String description, OffsetDateTime startsAt, int baseSeconds, int incrementSeconds,
            int entryFee, int maxPlayers, Division division, Kind kind, int repeatDays, int bonusPrize) {
        this.division = division;
        this.kind = kind;
        this.repeatDays = repeatDays;
        this.bonusPrize = bonusPrize;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.baseSeconds = baseSeconds;
        this.incrementSeconds = incrementSeconds;
        this.entryFee = entryFee;
        this.maxPlayers = maxPlayers;
    }

    void prizeItem(String itemId) {
        this.prizeItem = itemId;
    }

    public String getPrizeItem() {
        return prizeItem;
    }

    /** 개인 대회: 연 사람과 그가 미리 낸 상금(충전 paid, 운영자 free). */
    void hostedBy(long hostId, int bonusPaid, int bonusFree) {
        this.hostId = hostId;
        this.bonusPaid = bonusPaid;
        this.bonusFree = bonusFree;
    }

    /** 정기 리그의 다음 회차. */
    Tournament nextOccurrence(OffsetDateTime now) {
        Tournament t = new Tournament(createdBy == null ? 0 : createdBy, now);
        t.createdBy = createdBy;
        t.edit(title, description, startsAt.plusDays(repeatDays), baseSeconds, incrementSeconds, entryFee, maxPlayers,
                division, kind, repeatDays, bonusPrize);
        t.prizeItem = prizeItem;
        return t;
    }

    void start(OffsetDateTime now) {
        status = Status.RUNNING;
        currentRound = 1;
        nextRoundAt = null;
    }

    void roundDone(OffsetDateTime nextAt) {
        nextRoundAt = nextAt;
    }

    void nextRound() {
        currentRound++;
        nextRoundAt = null;
    }

    void finish(Long winnerId, int prize, OffsetDateTime now) {
        status = Status.FINISHED;
        this.winnerId = winnerId;
        this.prize = prize;
        this.nextRoundAt = null;
        this.finishedAt = now;
    }

    void cancel(OffsetDateTime now) {
        status = Status.CANCELLED;
        this.finishedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public int getBaseSeconds() {
        return baseSeconds;
    }

    public int getIncrementSeconds() {
        return incrementSeconds;
    }

    public int getEntryFee() {
        return entryFee;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Division getDivision() {
        return division;
    }

    public Kind getKind() {
        return kind;
    }

    public int getRepeatDays() {
        return repeatDays;
    }

    public Long getHostId() {
        return hostId;
    }

    public int getBonusPrize() {
        return bonusPrize;
    }

    public int getBonusPaid() {
        return bonusPaid;
    }

    public int getBonusFree() {
        return bonusFree;
    }

    public Status getStatus() {
        return status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public OffsetDateTime getNextRoundAt() {
        return nextRoundAt;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public int getPrize() {
        return prize;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
}
