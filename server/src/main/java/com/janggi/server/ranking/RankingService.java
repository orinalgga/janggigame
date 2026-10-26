package com.janggi.server.ranking;

import com.janggi.server.user.Grade;
import com.janggi.server.user.Look;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간·월간·연간 순위. 기간 안에 끝난 온라인 대국에서 오르내린 레이팅을 모두 더한 점수 순이다.
 * 같으면 많이 이긴 쪽, 그것도 같으면 적게 둔 쪽이 앞선다. 기간은 한국 시간으로 월요일, 1일, 1월 1일부터.
 * 정지된 회원은 빠진다.
 */
@Service
public class RankingService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TOP = 50;
    /** 몇 기간 전까지 볼 수 있는지. */
    public static final int MAX_OFFSET = 12;

    public enum Period {
        WEEK(3), MONTH(10), YEAR(30);

        /** 순위에 오르려면 기간 안에 둬야 하는 판 수. */
        final int minGames;

        Period(int minGames) {
            this.minGames = minGames;
        }

        public static Period parse(String s) {
            try {
                return valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new BadPeriod();
            }
        }

        LocalDate start(LocalDate today, int offset) {
            return switch (this) {
                case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
                case MONTH -> today.withDayOfMonth(1).minusMonths(offset);
                case YEAR -> today.withDayOfYear(1).minusYears(offset);
            };
        }

        LocalDate next(LocalDate start) {
            return switch (this) {
                case WEEK -> start.plusWeeks(1);
                case MONTH -> start.plusMonths(1);
                case YEAR -> start.plusYears(1);
            };
        }
    }

    public static class BadPeriod extends RuntimeException {
        BadPeriod() {
            super("기간은 week, month, year 중 하나이고 offset은 0~" + MAX_OFFSET + "이에요");
        }
    }

    /** rank는 순위에 오르지 못했으면(판 수가 모자라면) null. */
    public record Row(Integer rank, long userId, String nickname, Look look, String grade, int rating, int points, int games,
            int wins, int draws, int losses) {
    }

    /** from은 기간 첫날, to는 그다음 기간 첫날(이날은 빠진다). me는 로그인했고 그 기간에 둔 판이 있을 때만. */
    public record RankingDto(Period period, int offset, LocalDate from, LocalDate to, int minGames, List<Row> rows,
            Row me) {
    }

    private static final String SQL = """
            SELECT g.user_id, u.nickname, u.avatar, u.body, u.hair, u.beard, u.outfit, u.hat, u.rating, SUM(g.delta) AS points, COUNT(*) AS games,
                   SUM(CASE WHEN g.outcome = 1 THEN 1 ELSE 0 END) AS wins,
                   SUM(CASE WHEN g.outcome = 0 THEN 1 ELSE 0 END) AS draws,
                   SUM(CASE WHEN g.outcome = -1 THEN 1 ELSE 0 END) AS losses
            FROM (
                SELECT cho_user_id AS user_id, cho_rating_change AS delta,
                       CASE result WHEN 'CHO_WIN' THEN 1 WHEN 'HAN_WIN' THEN -1 ELSE 0 END AS outcome
                FROM games
                WHERE online = TRUE AND ended_at >= :from AND ended_at < :to
                  AND cho_user_id IS NOT NULL AND cho_rating_change IS NOT NULL
                UNION ALL
                SELECT han_user_id, han_rating_change,
                       CASE result WHEN 'HAN_WIN' THEN 1 WHEN 'CHO_WIN' THEN -1 ELSE 0 END
                FROM games
                WHERE online = TRUE AND ended_at >= :from AND ended_at < :to
                  AND han_user_id IS NOT NULL AND han_rating_change IS NOT NULL
            ) g
            JOIN users u ON u.id = g.user_id
            WHERE u.banned_at IS NULL
            GROUP BY g.user_id, u.nickname, u.avatar, u.body, u.hair, u.beard, u.outfit, u.hat, u.rating
            ORDER BY points DESC, wins DESC, games ASC, g.user_id ASC
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RankingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RankingDto rankings(Period period, int offset, Long userId) {
        if (offset < 0 || offset > MAX_OFFSET) {
            throw new BadPeriod();
        }
        LocalDate from = period.start(LocalDate.now(KST), offset);
        LocalDate to = period.next(from);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from.atStartOfDay(KST).toOffsetDateTime())
                .addValue("to", to.atStartOfDay(KST).toOffsetDateTime());
        List<Row> rows = new ArrayList<>();
        Row[] me = {null};
        jdbc.query(SQL, params, rs -> {
            int games = rs.getInt("games");
            boolean ranked = games >= period.minGames;
            int rating = rs.getInt("rating");
            Row row = new Row(ranked ? rows.size() + 1 : null, rs.getLong("user_id"), rs.getString("nickname"),
                    new Look(rs.getString("avatar"), rs.getString("body"), rs.getString("hair"),
                            rs.getString("beard"), rs.getString("outfit"), rs.getString("hat")),
                    Grade.name(rating), rating, rs.getInt("points"), games, rs.getInt("wins"), rs.getInt("draws"),
                    rs.getInt("losses"));
            if (ranked) {
                rows.add(row);
            }
            if (userId != null && row.userId() == userId) {
                me[0] = row;
            }
        });
        return new RankingDto(period, offset, from, to, period.minGames, rows.subList(0, Math.min(TOP, rows.size())),
                me[0]);
    }
}
