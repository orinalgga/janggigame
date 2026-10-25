package com.janggi.server.shop;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엽전 잔액과 기록. 충전한 엽전(paid)과 운영자가 준 엽전(free)을 따로 센다.
 * 살 때는 운영자가 준 엽전부터 쓰고, 운영자는 자기가 준 엽전만 회수할 수 있다.
 * 잔액은 JPA 엔티티에 두지 않고 회원 줄을 잠근 뒤(SELECT ... FOR UPDATE) 바꿔서 동시에 써도 어긋나지 않는다.
 */
@Service
public class CoinService {

    public enum Kind {
        /** 결제로 충전. */
        CHARGE,
        /** 상점에서 씀. */
        SPEND,
        /** 운영자가 넣거나 뺌(운영자 엽전). */
        ADMIN
    }

    public record Wallet(int paid, int free) {

        public int total() {
            return paid + free;
        }
    }

    /** delta는 전체 변동, freeDelta는 그중 운영자 엽전 몫, balance는 바뀐 뒤 전체 잔액. */
    public record LedgerRow(long id, int delta, int freeDelta, int balance, Kind kind, String ref, String memo,
            OffsetDateTime at) {
    }

    private final JdbcTemplate jdbc;

    public CoinService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Wallet wallet(long userId) {
        List<Wallet> r = jdbc.query("SELECT paid_coins, free_coins FROM users WHERE id = ?",
                (rs, i) -> new Wallet(rs.getInt(1), rs.getInt(2)), userId);
        return r.isEmpty() ? new Wallet(0, 0) : r.get(0);
    }

    @Transactional(readOnly = true)
    public int balance(long userId) {
        return wallet(userId).total();
    }

    /**
     * 엽전을 바꾸고 바뀐 지갑을 돌려준다. 부르는 쪽 트랜잭션 안에서 돈다.
     * CHARGE는 충전 엽전을 늘리고, SPEND(음수)는 운영자 엽전부터 쓰고, ADMIN은 운영자 엽전만 늘리거나 줄인다.
     * 모자라면 ShopException.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Wallet change(long userId, int delta, Kind kind, String ref, String memo) {
        Wallet before = jdbc.query("SELECT paid_coins, free_coins FROM users WHERE id = ? FOR UPDATE",
                        (rs, i) -> new Wallet(rs.getInt(1), rs.getInt(2)), userId)
                .stream().findFirst().orElseThrow(() -> new ShopException("회원을 찾을 수 없습니다"));
        int freeDelta = switch (kind) {
            case CHARGE -> 0;
            case ADMIN -> delta;
            case SPEND -> -Math.min(before.free(), -delta);
        };
        Wallet after = new Wallet(before.paid() + delta - freeDelta, before.free() + freeDelta);
        if (after.free() < 0) {
            throw new ShopException("운영자가 준 엽전은 " + before.free() + "닢만 남아 있어서 그만큼만 뺄 수 있어요");
        }
        if (after.paid() < 0) {
            throw new ShopException("엽전이 모자라요");
        }
        jdbc.update("UPDATE users SET paid_coins = ?, free_coins = ? WHERE id = ?", after.paid(), after.free(), userId);
        jdbc.update("INSERT INTO coin_ledger (user_id, delta, free_delta, balance, kind, ref, memo, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", userId, delta, freeDelta, after.total(), kind.name(), ref, memo,
                OffsetDateTime.now());
        return after;
    }

    /** 최근 50건, 새것부터. */
    @Transactional(readOnly = true)
    public List<LedgerRow> history(long userId) {
        return jdbc.query("SELECT id, delta, free_delta, balance, kind, ref, memo, created_at FROM coin_ledger"
                + " WHERE user_id = ? ORDER BY id DESC LIMIT 50",
                (rs, i) -> new LedgerRow(rs.getLong("id"), rs.getInt("delta"), rs.getInt("free_delta"),
                        rs.getInt("balance"), Kind.valueOf(rs.getString("kind")), rs.getString("ref"),
                        rs.getString("memo"), rs.getObject("created_at", OffsetDateTime.class)),
                userId);
    }
}
