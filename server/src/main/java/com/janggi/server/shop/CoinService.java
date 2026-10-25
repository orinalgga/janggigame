package com.janggi.server.shop;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엽전 잔액과 기록. 잔액은 users.coins 한 칸이고, 바꿀 때는 한 줄 UPDATE로
 * "모자라면 바꾸지 않기"까지 한 번에 해서 동시에 두 번 써도 음수가 되지 않는다.
 */
@Service
public class CoinService {

    public enum Kind {
        /** 결제로 충전. */
        CHARGE,
        /** 상점에서 씀. */
        SPEND,
        /** 관리자가 넣거나 뺌. */
        ADMIN
    }

    public record LedgerRow(long id, int delta, int balance, Kind kind, String ref, String memo,
            OffsetDateTime at) {
    }

    private final JdbcTemplate jdbc;

    public CoinService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public int balance(long userId) {
        List<Integer> r = jdbc.queryForList("SELECT coins FROM users WHERE id = ?", Integer.class, userId);
        return r.isEmpty() ? 0 : r.get(0);
    }

    /** 잔액을 delta만큼 바꾸고 바뀐 잔액을 돌려준다. 모자라면 ShopException. 부르는 쪽 트랜잭션 안에서 돈다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int change(long userId, int delta, Kind kind, String ref, String memo) {
        int updated = jdbc.update("UPDATE users SET coins = coins + ? WHERE id = ? AND coins + ? >= 0",
                delta, userId, delta);
        if (updated == 0) {
            throw new ShopException("엽전이 모자라요");
        }
        int balance = balance(userId);
        jdbc.update("INSERT INTO coin_ledger (user_id, delta, balance, kind, ref, memo, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)", userId, delta, balance, kind.name(), ref, memo, OffsetDateTime.now());
        return balance;
    }

    /** 최근 50건, 새것부터. */
    @Transactional(readOnly = true)
    public List<LedgerRow> history(long userId) {
        return jdbc.query("SELECT id, delta, balance, kind, ref, memo, created_at FROM coin_ledger"
                + " WHERE user_id = ? ORDER BY id DESC LIMIT 50",
                (rs, i) -> new LedgerRow(rs.getLong("id"), rs.getInt("delta"), rs.getInt("balance"),
                        Kind.valueOf(rs.getString("kind")), rs.getString("ref"), rs.getString("memo"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                userId);
    }
}
