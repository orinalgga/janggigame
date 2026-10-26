package com.janggi.server.admin;

import com.janggi.core.GameResult;
import com.janggi.server.match.MatchService;
import com.janggi.server.notice.NoticeService;
import com.janggi.server.tournament.TournamentService;
import com.janggi.server.record.GameRecordRepository;
import com.janggi.server.shop.CoinPack;
import com.janggi.server.shop.CoinService;
import com.janggi.server.shop.OwnedItem;
import com.janggi.server.shop.OwnedItemRepository;
import com.janggi.server.shop.Purchase;
import com.janggi.server.shop.PurchaseRepository;
import com.janggi.server.shop.ShopException;
import com.janggi.server.shop.ShopItem;
import com.janggi.server.user.AuthIdentity;
import com.janggi.server.user.AuthIdentityRepository;
import com.janggi.server.user.Grade;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import com.janggi.server.user.UserService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 기능. 관리자는 JANGGI_ADMIN_EMAILS에 적은 메일로 로그인한 회원이다.
 * '오늘'은 한국 시간 자정부터 센다.
 */
@Service
public class AdminService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 20;
    private static final int MAX_REASON = 200;

    public record StatsDto(long users, long usersToday, long gamesToday, long onlinePlaying, int waiting,
            long paidOrders, long revenueTotal, long revenueToday) {
    }

    public record UserRow(long id, String nickname, boolean nicknameChosen, int rating, String grade,
            OffsetDateTime createdAt,
            List<String> emails, boolean banned, String banReason, List<String> items, boolean admin, int coins,
            int freeCoins) {
    }

    public record UserPage(List<UserRow> users, int page, int totalPages, long total) {
    }

    public record LogRow(OffsetDateTime at, long adminId, String admin, AdminLog.Action action, Long targetId,
            String target, String detail) {
    }

    public record OrderRow(String orderId, long userId, String nickname, String itemId, String itemTitle,
            int amount, String provider, Purchase.Status status, OffsetDateTime createdAt, OffsetDateTime paidAt) {
    }

    private final Set<String> adminEmails;
    private final UserRepository users;
    private final UserService userService;
    private final AuthIdentityRepository identities;
    private final GameRecordRepository games;
    private final PurchaseRepository purchases;
    private final OwnedItemRepository owned;
    private final MatchService match;
    private final AdminLogRepository logs;
    private final NoticeService notices;
    private final CoinService coins;
    private final TournamentService tournaments;

    public AdminService(@Value("${janggi.admin-emails:}") String adminEmails, UserRepository users,
            UserService userService, AuthIdentityRepository identities, GameRecordRepository games,
            PurchaseRepository purchases, OwnedItemRepository owned, MatchService match, AdminLogRepository logs,
            NoticeService notices, CoinService coins, TournamentService tournaments) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
        this.users = users;
        this.userService = userService;
        this.identities = identities;
        this.games = games;
        this.purchases = purchases;
        this.owned = owned;
        this.match = match;
        this.logs = logs;
        this.notices = notices;
        this.coins = coins;
        this.tournaments = tournaments;
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        return userId != null && !adminEmails.isEmpty() && isAdmin(identities.findByUserId(userId));
    }

    private boolean isAdmin(List<AuthIdentity> ids) {
        return ids.stream().anyMatch(i -> i.getEmail() != null
                && adminEmails.contains(i.getEmail().toLowerCase(Locale.ROOT)));
    }

    /** 관리자가 아니면 AdminException(403). */
    public void requireAdmin(Long userId) {
        if (!isAdmin(userId)) {
            throw new AdminException.Forbidden();
        }
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        OffsetDateTime today = OffsetDateTime.now(KST).toLocalDate().atStartOfDay(KST).toOffsetDateTime();
        return new StatsDto(users.count(), users.countByCreatedAtGreaterThanEqual(today),
                games.countByStartedAtGreaterThanEqual(today), games.countByOnlineTrueAndResult(GameResult.ONGOING),
                match.waitingCount(), purchases.countByStatus(Purchase.Status.PAID),
                purchases.paidSince(OffsetDateTime.parse("2000-01-01T00:00:00Z")), purchases.paidSince(today));
    }

    @Transactional(readOnly = true)
    public UserPage users(String query, int page) {
        String q = query == null ? "" : query.strip();
        long id = q.matches("\\d{1,18}") ? Long.parseLong(q) : -1;
        Page<User> result = users.search(q, id,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
        return new UserPage(result.map(this::row).toList(), result.getNumber(), result.getTotalPages(),
                result.getTotalElements());
    }

    @Transactional
    public UserRow rename(long adminId, long userId, String nickname) {
        String before = user(userId).getNickname();
        User user = userService.changeNickname(userId, nickname);
        log(adminId, AdminLog.Action.RENAME, userId, before + " → " + user.getNickname());
        return row(user);
    }

    @Transactional
    public UserRow ban(long adminId, long userId, String reason) {
        User user = user(userId);
        if (isAdmin(userId)) {
            throw new AdminException.BadRequest("관리자는 정지할 수 없어요");
        }
        String r = reason == null || reason.isBlank() ? "운영 정책 위반" : reason.strip();
        user.ban(r.length() > MAX_REASON ? r.substring(0, MAX_REASON) : r, OffsetDateTime.now());
        match.leave(userId);
        log(adminId, AdminLog.Action.BAN, userId, user.getBanReason());
        return row(user);
    }

    @Transactional
    public UserRow unban(long adminId, long userId) {
        User user = user(userId);
        user.unban();
        log(adminId, AdminLog.Action.UNBAN, userId, null);
        return row(user);
    }

    /** 결제 없이 테마를 넣어 준다(보상, 환불 대신 등). */
    @Transactional
    public UserRow grant(long adminId, long userId, String itemId) {
        User user = user(userId);
        ShopItem item = ShopItem.byId(itemId).orElseThrow(() -> new AdminException.BadRequest("없는 상품입니다"));
        if (item.free() || owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new AdminException.BadRequest("이미 가지고 있는 상품입니다");
        }
        owned.save(new OwnedItem(userId, item.id(), OffsetDateTime.now()));
        log(adminId, AdminLog.Action.GRANT, userId, item.title());
        return row(user);
    }

    @Transactional
    public List<TournamentService.SummaryDto> createTournament(long adminId, TournamentService.TournamentRequest req) {
        List<TournamentService.SummaryDto> made = tournaments.create(adminId, req);
        made.forEach(t -> log(adminId, AdminLog.Action.TOURNAMENT_CREATE, null, t.title()));
        return made;
    }

    @Transactional
    public TournamentService.SummaryDto updateTournament(long adminId, long id, TournamentService.TournamentRequest req) {
        TournamentService.SummaryDto t = tournaments.update(id, req);
        log(adminId, AdminLog.Action.TOURNAMENT_UPDATE, null, t.title());
        return t;
    }

    @Transactional
    public TournamentService.SummaryDto cancelTournament(long adminId, long id) {
        TournamentService.SummaryDto t = tournaments.cancel(id);
        log(adminId, AdminLog.Action.TOURNAMENT_CANCEL, null, t.title() + " (참가비 " + t.players() + "명 환불)");
        return t;
    }

    /** 운영자 엽전을 넣거나(양수) 뺀다(음수). 충전한 엽전은 뺄 수 없다. 사유는 회원의 엽전 기록에도 남는다. */
    @Transactional
    public UserRow giveCoins(long adminId, long userId, int amount, String memo) {
        User user = user(userId);
        if (amount == 0 || Math.abs(amount) > 100_000) {
            throw new AdminException.BadRequest("엽전은 1~100000닢 사이로 넣거나 빼 주세요");
        }
        String m = memo == null || memo.isBlank() ? (amount > 0 ? "운영자 지급" : "운영자 회수") : memo.strip();
        m = m.length() > MAX_REASON ? m.substring(0, MAX_REASON) : m;
        CoinService.Wallet w;
        try {
            w = coins.change(userId, amount, CoinService.Kind.ADMIN, "admin:" + adminId, m);
        } catch (ShopException e) {
            throw new AdminException.BadRequest(e.getMessage());
        }
        log(adminId, AdminLog.Action.COINS, userId,
                (amount > 0 ? "+" : "") + amount + "닢 (운영자 엽전 " + w.free() + ", 전체 " + w.total() + ") " + m);
        return row(user);
    }

    /** 최근 관리자 작업 100건. 닉네임은 지금 닉네임이다. */
    @Transactional(readOnly = true)
    public List<LogRow> logs() {
        List<AdminLog> list = logs.findTop100ByOrderByCreatedAtDescIdDesc();
        Map<Long, String> names = users.findAllById(list.stream()
                        .flatMap(l -> Stream.of(l.getAdminId(), l.getTargetUserId()))
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        return list.stream()
                .map(l -> new LogRow(l.getCreatedAt(), l.getAdminId(), names.getOrDefault(l.getAdminId(), "?"),
                        l.getAction(), l.getTargetUserId(),
                        l.getTargetUserId() == null ? null : names.getOrDefault(l.getTargetUserId(), "?"),
                        l.getDetail()))
                .toList();
    }

    @Transactional
    public NoticeService.NoticeDto createNotice(long adminId, NoticeService.NoticeRequest req) {
        NoticeService.NoticeDto n = notices.create(adminId, req);
        log(adminId, AdminLog.Action.NOTICE_CREATE, null, n.title());
        return n;
    }

    @Transactional
    public NoticeService.NoticeDto updateNotice(long adminId, long noticeId, NoticeService.NoticeRequest req) {
        NoticeService.NoticeDto n = notices.update(noticeId, req);
        log(adminId, AdminLog.Action.NOTICE_UPDATE, null, n.title());
        return n;
    }

    @Transactional
    public void deleteNotice(long adminId, long noticeId) {
        log(adminId, AdminLog.Action.NOTICE_DELETE, null, notices.delete(noticeId));
    }

    private void log(long adminId, AdminLog.Action action, Long targetUserId, String detail) {
        logs.save(new AdminLog(adminId, action, targetUserId, detail, OffsetDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<OrderRow> orders() {
        List<Purchase> list = purchases.findTop100ByOrderByCreatedAtDesc();
        Map<Long, String> names = users.findAllById(list.stream().map(Purchase::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        return list.stream()
                .map(p -> new OrderRow(p.getOrderId(), p.getUserId(), names.getOrDefault(p.getUserId(), "(탈퇴)"),
                        p.getItemId(), title(p.getItemId()),
                        p.getAmount(), p.getProvider(), p.getStatus(), p.getCreatedAt(), p.getPaidAt()))
                .toList();
    }

    /** 주문 상품 이름. 지금은 엽전 충전이고, 예전 주문은 테마다. */
    private static String title(String itemId) {
        return CoinPack.byId(itemId).map(CoinPack::title)
                .or(() -> ShopItem.byId(itemId).map(ShopItem::title))
                .orElse(itemId);
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> new AdminException.BadRequest("회원을 찾을 수 없습니다"));
    }

    private UserRow row(User u) {
        List<AuthIdentity> ids = identities.findByUserId(u.getId());
        List<String> emails = ids.stream().map(AuthIdentity::getEmail).filter(Objects::nonNull).toList();
        List<String> items = owned.findByUserId(u.getId()).stream().map(OwnedItem::getItemId).toList();
        CoinService.Wallet wallet = coins.wallet(u.getId());
        return new UserRow(u.getId(), u.getNickname(), u.isNicknameChosen(), u.getRating(), Grade.name(u.getRating()),
                u.getCreatedAt(),
                emails, u.isBanned(), u.getBanReason(), items, isAdmin(ids), wallet.total(),
                wallet.free());
    }
}
