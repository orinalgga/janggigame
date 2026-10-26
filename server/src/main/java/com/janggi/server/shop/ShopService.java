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

    /** price는 엽전. reward면 팔지 않는 대회 보상. owned, equipped는 로그인하지 않았으면 무료 상품과 기본 장착 기준이다. */
    public record ItemDto(String id, ShopItem.Slot slot, String title, String description, int price,
            boolean reward, boolean owned, boolean equipped) {
    }

    /** price는 원. */
    public record PackDto(String id, String title, int coins, int price) {
    }

    /**
     * 세트. fullPrice는 부품 값의 합, price는 깎은 값. 로그인했으면 price는 아직 없는 부품만 센 값이고,
     * owned는 부품을 다 가진 세트다.
     */
    public record BundleDto(String id, String title, String description, List<String> items, int fullPrice, int price,
            boolean owned) {
    }

    /** coins는 전체 엽전, freeCoins는 그중 운영자가 준 엽전. 로그인하지 않았으면 둘 다 null. */
    public record ShopDto(Integer coins, Integer freeCoins, List<ItemDto> items, List<BundleDto> bundles,
            List<PackDto> packs) {
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
        Set<String> worn = user == null
                ? Arrays.stream(ShopItem.values()).filter(ShopItem::starter).map(ShopItem::id).collect(Collectors.toSet())
                : Set.of(user.getAvatar(), user.getBody(), user.getHair(), user.getBeard(), user.getOutfit(), user.getHat());
        List<ItemDto> items = Arrays.stream(ShopItem.values())
                .map(i -> new ItemDto(i.id(), i.slot(), i.title(), i.description(), i.price(), i.reward(),
                        i.free() || mine.contains(i.id()),
                        i.id().equals(board) || i.id().equals(pieces) || worn.contains(i.id())))
                .toList();
        List<BundleDto> bundles = Arrays.stream(ShopBundle.values()).map(b -> {
            List<ShopItem> missing = b.items().stream().filter(i -> !i.free() && !mine.contains(i.id())).toList();
            int full = missing.stream().mapToInt(ShopItem::price).sum();
            return new BundleDto(b.id(), b.title(), b.description(), b.items().stream().map(ShopItem::id).toList(),
                    full, ShopBundle.discounted(full), missing.isEmpty());
        }).toList();
        CoinService.Wallet wallet = user == null ? null : coins.wallet(user.getId());
        return new ShopDto(wallet == null ? null : wallet.total(), wallet == null ? null : wallet.free(), items, bundles,
                PACKS);
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

    /** 엽전으로 테마나 아바타를 산다. 모자라면 ShopException. */
    @Transactional
    public ShopDto buy(long userId, String itemId) {
        ShopItem item = item(itemId);
        if (item.reward()) {
            throw new ShopException("대회 우승으로만 얻을 수 있는 상품입니다");
        }
        if (item.free() || owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new ShopException("이미 가지고 있는 상품입니다");
        }
        coins.change(userId, -item.price(), CoinService.Kind.SPEND, item.id(), item.title());
        // 같은 상품을 동시에 두 번 사면 (user_id, item_id) 유일 제약에 걸려 엽전 차감까지 되돌린다.
        owned.saveAndFlush(new OwnedItem(userId, item.id(), OffsetDateTime.now()));
        return shop(userId);
    }

    /** 세트를 산다. 이미 가진 부품은 빼고 남은 부품 값을 깎아 받은 뒤, 세트를 통째로 입힌다. */
    @Transactional
    public ShopDto buyBundle(long userId, String bundleId) {
        ShopBundle bundle = ShopBundle.byId(bundleId).orElseThrow(() -> new ShopException("없는 세트입니다"));
        List<ShopItem> missing = bundle.items().stream()
                .filter(i -> !i.free() && !owned.existsByUserIdAndItemId(userId, i.id()))
                .toList();
        if (missing.isEmpty()) {
            throw new ShopException("이미 세트를 모두 가지고 있습니다");
        }
        int price = ShopBundle.discounted(missing.stream().mapToInt(ShopItem::price).sum());
        coins.change(userId, -price, CoinService.Kind.SPEND, bundle.id(), bundle.title());
        OffsetDateTime now = OffsetDateTime.now();
        for (ShopItem i : missing) {
            owned.save(new OwnedItem(userId, i.id(), now));
        }
        owned.flush();
        for (ShopItem i : bundle.items()) {
            equip(userId, i.id());
        }
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
            case AVATAR -> user.equipAvatar(item.id());
            case BODY -> user.equipBody(item.id());
            case HAIR -> user.equipHair(item.id());
            case BEARD -> user.equipBeard(item.id());
            case OUTFIT -> user.equipOutfit(item.id());
            case HAT -> user.equipHat(item.id());
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
