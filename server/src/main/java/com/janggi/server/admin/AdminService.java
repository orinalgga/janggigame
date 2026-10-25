package com.janggi.server.admin;

import com.janggi.core.GameResult;
import com.janggi.server.match.MatchService;
import com.janggi.server.record.GameRecordRepository;
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
import java.util.stream.Collectors;
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
            List<String> emails, boolean banned, String banReason, List<String> items, boolean admin) {
    }

    public record UserPage(List<UserRow> users, int page, int totalPages, long total) {
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

    public AdminService(@Value("${janggi.admin-emails:}") String adminEmails, UserRepository users,
            UserService userService, AuthIdentityRepository identities, GameRecordRepository games,
            PurchaseRepository purchases, OwnedItemRepository owned, MatchService match) {
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
    public UserRow rename(long userId, String nickname) {
        return row(userService.changeNickname(userId, nickname));
    }

    @Transactional
    public UserRow ban(long userId, String reason) {
        User user = user(userId);
        if (isAdmin(userId)) {
            throw new AdminException.BadRequest("관리자는 정지할 수 없어요");
        }
        String r = reason == null || reason.isBlank() ? "운영 정책 위반" : reason.strip();
        user.ban(r.length() > MAX_REASON ? r.substring(0, MAX_REASON) : r, OffsetDateTime.now());
        match.leave(userId);
        return row(user);
    }

    @Transactional
    public UserRow unban(long userId) {
        User user = user(userId);
        user.unban();
        return row(user);
    }

    /** 결제 없이 테마를 넣어 준다(보상, 환불 대신 등). */
    @Transactional
    public UserRow grant(long userId, String itemId) {
        User user = user(userId);
        ShopItem item = ShopItem.byId(itemId).orElseThrow(() -> new AdminException.BadRequest("없는 상품입니다"));
        if (item.free() || owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new AdminException.BadRequest("이미 가지고 있는 상품입니다");
        }
        owned.save(new OwnedItem(userId, item.id(), OffsetDateTime.now()));
        return row(user);
    }

    @Transactional(readOnly = true)
    public List<OrderRow> orders() {
        List<Purchase> list = purchases.findTop100ByOrderByCreatedAtDesc();
        Map<Long, String> names = users.findAllById(list.stream().map(Purchase::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        return list.stream()
                .map(p -> new OrderRow(p.getOrderId(), p.getUserId(), names.getOrDefault(p.getUserId(), "(탈퇴)"),
                        p.getItemId(), ShopItem.byId(p.getItemId()).map(ShopItem::title).orElse(p.getItemId()),
                        p.getAmount(), p.getProvider(), p.getStatus(), p.getCreatedAt(), p.getPaidAt()))
                .toList();
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> new AdminException.BadRequest("회원을 찾을 수 없습니다"));
    }

    private UserRow row(User u) {
        List<AuthIdentity> ids = identities.findByUserId(u.getId());
        List<String> emails = ids.stream().map(AuthIdentity::getEmail).filter(e -> e != null).toList();
        List<String> items = owned.findByUserId(u.getId()).stream().map(OwnedItem::getItemId).toList();
        return new UserRow(u.getId(), u.getNickname(), u.isNicknameChosen(), u.getRating(), Grade.name(u.getRating()),
                u.getCreatedAt(),
                emails, u.isBanned(), u.getBanReason(), items, isAdmin(ids));
    }
}
