package com.janggi.server.megaphone;

import com.janggi.server.shop.CoinService;
import com.janggi.server.user.Look;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 확성기. 엽전으로 사서 users.megaphones에 모아 두고, 하나 쓰면 한 줄을 /topic/megaphone으로 모두에게 알린다.
 * 개수는 엽전처럼 JPA 엔티티에 두지 않고 한 줄 UPDATE로만 바꾼다(동시에 써도 어긋나지 않게).
 */
@Service
public class MegaphoneService {

    public static final String TOPIC = "/topic/megaphone";
    public static final int MAX_LENGTH = 60;
    private static final int RECENT = 20;
    private static final Duration KEEP = Duration.ofHours(24);

    public enum Pack {
        ONE("megaphone.1", "확성기 1개", 1, 5),
        TEN("megaphone.10", "확성기 10개", 10, 45);

        private final String id;
        private final String title;
        private final int count;
        private final int price;

        Pack(String id, String title, int count, int price) {
            this.id = id;
            this.title = title;
            this.count = count;
            this.price = price;
        }

        static Optional<Pack> byId(String id) {
            return Arrays.stream(values()).filter(p -> p.id.equals(id)).findFirst();
        }
    }

    /** price는 엽전. */
    public record PackDto(String id, String title, int count, int price) {
    }

    public record PostDto(long id, long userId, String nickname, Look look, String text, OffsetDateTime at) {
    }

    /** count는 내가 가진 확성기(로그인하지 않았으면 null), recent는 지난 하루 동안 쓴 것 새것부터. */
    public record StateDto(Integer count, List<PostDto> recent, List<PackDto> packs, int maxLength) {
    }

    /** 실시간 알림. 새로 쓴 것이면 post, 운영자가 내린 것이면 hidden에 그 id. */
    public record Event(PostDto post, Long hidden) {
    }

    private static final List<PackDto> PACKS = Arrays.stream(Pack.values())
            .map(p -> new PackDto(p.id, p.title, p.count, p.price))
            .toList();

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final CoinService coins;
    private final SimpMessagingTemplate messaging;
    /** 한 사람이 다시 쓰려면 기다려야 하는 시간. */
    private final Duration cooldown;

    public MegaphoneService(JdbcTemplate jdbc, UserRepository users, CoinService coins, SimpMessagingTemplate messaging,
            @Value("${janggi.megaphone.cooldown-seconds:30}") long cooldownSeconds) {
        this.jdbc = jdbc;
        this.users = users;
        this.coins = coins;
        this.messaging = messaging;
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
    }

    @Transactional(readOnly = true)
    public StateDto state(Long userId) {
        return new StateDto(userId == null ? null : count(userId), recent(), PACKS, MAX_LENGTH);
    }

    @Transactional
    public StateDto buy(long userId, String packId) {
        Pack pack = Pack.byId(packId).orElseThrow(() -> new MegaphoneException("없는 상품입니다"));
        coins.change(userId, -pack.price, CoinService.Kind.SPEND, pack.id, pack.title);
        jdbc.update("UPDATE users SET megaphones = megaphones + ? WHERE id = ?", pack.count, userId);
        return state(userId);
    }

    /** 확성기 하나를 써서 모두에게 알린다. 없거나, 너무 자주 쓰거나, 쓸 수 없는 말이면 MegaphoneException. */
    @Transactional
    public PostDto post(long userId, String text) {
        String clean = text == null ? "" : text.strip().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            throw new MegaphoneException("외칠 말을 입력하세요");
        }
        if (clean.codePointCount(0, clean.length()) > MAX_LENGTH) {
            throw new MegaphoneException("확성기는 " + MAX_LENGTH + "자까지 쓸 수 있어요");
        }
        if (Profanity.rude(clean)) {
            throw new MegaphoneException("욕설이나 비하하는 말은 쓸 수 없어요");
        }
        if (Profanity.link(clean)) {
            throw new MegaphoneException("주소나 메신저 안내는 쓸 수 없어요");
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime last = jdbc.query(
                "SELECT created_at FROM megaphone_posts WHERE user_id = ? ORDER BY created_at DESC LIMIT 1",
                (rs, i) -> rs.getObject(1, OffsetDateTime.class), userId).stream().findFirst().orElse(null);
        if (last != null && last.plus(cooldown).isAfter(now)) {
            long wait = Duration.between(now, last.plus(cooldown)).toSeconds() + 1;
            throw new MegaphoneException(wait + "초 뒤에 다시 쓸 수 있어요");
        }
        if (jdbc.update("UPDATE users SET megaphones = megaphones - 1 WHERE id = ? AND megaphones > 0", userId) == 0) {
            throw new MegaphoneException("확성기가 없어요. 먼저 사 주세요");
        }
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO megaphone_posts (user_id, text, created_at) VALUES (?, ?, ?)", new String[] {"id"});
            ps.setLong(1, userId);
            ps.setString(2, clean);
            ps.setTimestamp(3, Timestamp.from(now.toInstant()));
            return ps;
        }, key);
        User user = users.findById(userId).orElseThrow(() -> new MegaphoneException("회원을 찾을 수 없습니다"));
        PostDto dto = new PostDto(key.getKey().longValue(), userId, user.getNickname(), Look.of(user), clean, now);
        afterCommit(new Event(dto, null));
        return dto;
    }

    /** 운영자가 확성기 하나를 내린다. 화면에 떠 있으면 바로 사라진다. 관리자 기록에 남길 요약을 돌려준다. */
    @Transactional
    public String hide(long id) {
        List<String> found = jdbc.query("SELECT u.nickname, m.text FROM megaphone_posts m JOIN users u ON u.id = m.user_id"
                + " WHERE m.id = ? AND m.hidden_at IS NULL", (rs, i) -> rs.getString(1) + ": " + rs.getString(2), id);
        if (found.isEmpty()) {
            throw new MegaphoneException("없거나 이미 내린 확성기입니다");
        }
        jdbc.update("UPDATE megaphone_posts SET hidden_at = ? WHERE id = ?", Timestamp.from(OffsetDateTime.now().toInstant()), id);
        afterCommit(new Event(null, id));
        return found.get(0);
    }

    private int count(long userId) {
        return jdbc.query("SELECT megaphones FROM users WHERE id = ?", (rs, i) -> rs.getInt(1), userId)
                .stream().findFirst().orElse(0);
    }

    private List<PostDto> recent() {
        record Row(long id, long userId, String text, OffsetDateTime at) {
        }
        List<Row> rows = jdbc.query("SELECT id, user_id, text, created_at FROM megaphone_posts"
                        + " WHERE hidden_at IS NULL AND created_at > ? ORDER BY created_at DESC, id DESC LIMIT " + RECENT,
                (rs, i) -> new Row(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getObject(4, OffsetDateTime.class)),
                Timestamp.from(OffsetDateTime.now().minus(KEEP).toInstant()));
        Map<Long, User> who = users.findAllById(rows.stream().map(Row::userId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return rows.stream().filter(r -> who.containsKey(r.userId()))
                .map(r -> new PostDto(r.id(), r.userId(), who.get(r.userId()).getNickname(), Look.of(who.get(r.userId())),
                        r.text(), r.at()))
                .toList();
    }

    /** 저장이 끝난 뒤에 알린다. 되돌려진 글이 화면에 뜨지 않게. */
    private void afterCommit(Event event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messaging.convertAndSend(TOPIC, event);
            }
        });
    }
}
