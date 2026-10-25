package com.janggi.server.shop;

import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점. 돈으로는 엽전만 충전하고(100원 = 1닢), 테마는 엽전으로 산다.
 */
@Service
public class ShopService {

    /** price는 엽전. owned, equipped는 로그인하지 않았으면 무료 상품과 기본 장착 기준이다. */
    public record ItemDto(String id, ShopItem.Slot slot, String title, String description, int price,
            boolean owned, boolean equipped) {
    }

    /** price는 원. */
    public record PackDto(String id, String title, int coins, int price) {
    }

    /** coins는 로그인하지 않았으면 null. */
    public record ShopDto(Integer coins, List<ItemDto> items, List<PackDto> packs) {
    }

    public record OrderDto(String orderId, String packId, String title, int coins, int amount, String provider) {
    }

    private static final List<PackDto> PACKS = Arrays.stream(CoinPack.values())
            .map(p -> new PackDto(p.id(), p.title(), p.coins(), p.price()))
            .toList();

    private final UserRepository users;
    private final OwnedItemRepository owned;
    private final PurchaseRepository purchases;
    private final PaymentGateway gateway;
    private final CoinService coins;

    public ShopService(UserRepository users, OwnedItemRepository owned, PurchaseRepository purchases,
            PaymentGateway gateway, CoinService coins) {
        this.users = users;
        this.owned = owned;
        this.purchases = purchases;
        this.gateway = gateway;
        this.coins = coins;
    }

    @Transactional(readOnly = true)
    public ShopDto shop(Long userId) {
        User user = userId == null ? null : users.findById(userId).orElse(null);
        Set<String> mine = user == null ? Set.of()
                : owned.findByUserId(user.getId()).stream().map(OwnedItem::getItemId).collect(Collectors.toSet());
        String board = user == null ? ShopItem.BOARD_CLASSIC.id() : user.getBoardTheme();
        String pieces = user == null ? ShopItem.PIECES_CLASSIC.id() : user.getPieceTheme();
        List<ItemDto> items = Arrays.stream(ShopItem.values())
                .map(i -> new ItemDto(i.id(), i.slot(), i.title(), i.description(), i.price(),
                        i.free() || mine.contains(i.id()),
                        i.id().equals(board) || i.id().equals(pieces)))
                .toList();
        return new ShopDto(user == null ? null : coins.balance(user.getId()), items, PACKS);
    }

    /** 엽전 충전 결제를 시작한다. 결제창에는 여기서 받은 orderId와 금액을 그대로 넘긴다. */
    @Transactional
    public OrderDto order(long userId, String packId) {
        CoinPack pack = CoinPack.byId(packId).orElseThrow(() -> new ShopException("없는 충전 상품입니다"));
        Purchase p = purchases.save(new Purchase(UUID.randomUUID().toString(), userId, pack, gateway.name(),
                OffsetDateTime.now()));
        return new OrderDto(p.getOrderId(), pack.id(), pack.title(), pack.coins(), p.getAmount(), p.getProvider());
    }

    /**
     * 결제사 승인을 받고 엽전을 넣어 준다. 같은 주문을 두 번 확인해도 한 번만 들어간다.
     * 승인이 거절되면 주문을 실패로 남기고 ShopException을 던진다.
     */
    @Transactional(noRollbackFor = ShopException.class)
    public ShopDto confirm(long userId, String orderId, String paymentKey) {
        Purchase p = purchases.findById(orderId)
                .filter(x -> x.getUserId() == userId)
                .orElseThrow(() -> new ShopException("주문을 찾을 수 없습니다"));
        if (p.getStatus() == Purchase.Status.FAILED) {
            throw new ShopException("실패한 주문입니다. 다시 주문해 주세요");
        }
        if (p.getStatus() == Purchase.Status.PENDING) {
            CoinPack pack = CoinPack.byId(p.getItemId())
                    .orElseThrow(() -> new ShopException("더 이상 받지 않는 주문입니다. 다시 주문해 주세요"));
            try {
                gateway.confirm(p.getOrderId(), paymentKey, p.getAmount());
            } catch (ShopException e) {
                p.failed();
                throw e;
            }
            p.paid(paymentKey, OffsetDateTime.now());
            coins.change(userId, pack.coins(), CoinService.Kind.CHARGE, p.getOrderId(), pack.title());
        }
        return shop(userId);
    }

    /** 엽전으로 테마를 산다. 모자라면 ShopException. */
    @Transactional
    public ShopDto buy(long userId, String itemId) {
        ShopItem item = item(itemId);
        if (item.free() || owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new ShopException("이미 가지고 있는 상품입니다");
        }
        coins.change(userId, -item.price(), CoinService.Kind.SPEND, item.id(), item.title());
        // 같은 상품을 동시에 두 번 사면 (user_id, item_id) 유일 제약에 걸려 엽전 차감까지 되돌린다.
        owned.saveAndFlush(new OwnedItem(userId, item.id(), OffsetDateTime.now()));
        return shop(userId);
    }

    @Transactional
    public ShopDto equip(long userId, String itemId) {
        ShopItem item = item(itemId);
        if (!item.free() && !owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new ShopException("먼저 상품을 사야 장착할 수 있습니다");
        }
        User user = users.findById(userId).orElseThrow(() -> new ShopException("회원을 찾을 수 없습니다"));
        switch (item.slot()) {
            case BOARD -> user.equipBoard(item.id());
            case PIECES -> user.equipPieces(item.id());
        }
        return shop(userId);
    }

    public List<CoinService.LedgerRow> history(long userId) {
        return coins.history(userId);
    }

    private static ShopItem item(String id) {
        return ShopItem.byId(id).orElseThrow(() -> new ShopException("없는 상품입니다"));
    }
}
