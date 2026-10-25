package com.janggi.server.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    public static final int INITIAL_RATING = 1200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    /** false면 가입할 때 받은 임시 닉네임(장기꾼+번호)이다. */
    @Column(name = "nickname_chosen", nullable = false)
    private boolean nicknameChosen;

    @Column(nullable = false)
    private int rating;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** 관리자가 이용을 정지한 때. null이면 정상. */
    @Column(name = "banned_at")
    private OffsetDateTime bannedAt;

    @Column(name = "ban_reason")
    private String banReason;

    /** 장착한 장기판 테마(상점 상품 id). */
    @Column(name = "board_theme", nullable = false)
    private String boardTheme = "board.classic";

    /** 장착한 장기알 테마(상점 상품 id). */
    @Column(name = "piece_theme", nullable = false)
    private String pieceTheme = "pieces.classic";

    protected User() {
    }

    public User(String nickname, OffsetDateTime createdAt) {
        this.nickname = nickname;
        this.rating = INITIAL_RATING;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isNicknameChosen() {
        return nicknameChosen;
    }

    void rename(String nickname, boolean chosen) {
        this.nickname = nickname;
        this.nicknameChosen = chosen;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isBanned() {
        return bannedAt != null;
    }

    public String getBanReason() {
        return banReason;
    }

    public void ban(String reason, OffsetDateTime at) {
        this.bannedAt = at;
        this.banReason = reason;
    }

    public void unban() {
        this.bannedAt = null;
        this.banReason = null;
    }

    public int getRating() {
        return rating;
    }

    public String getBoardTheme() {
        return boardTheme;
    }

    public String getPieceTheme() {
        return pieceTheme;
    }

    public void equipBoard(String itemId) {
        this.boardTheme = itemId;
    }

    public void equipPieces(String itemId) {
        this.pieceTheme = itemId;
    }

    public void changeRating(int delta) {
        this.rating += delta;
    }
}
