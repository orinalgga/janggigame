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

@Service
public class ShopService {

    /** owned, equipped는 로그인하지 않았으면 무료 상품과 기본 장착 기준이다. */
    public record ItemDto(String id, ShopItem.Slot slot, String title, String description, int price,
            boolean owned, boolean equipped) {
    }

    public record OrderDto(String orderId, String itemId, String title, int amount, String provider) {
    }

    private final UserRepository users;
    private final OwnedItemRepository owned;
    private final PurchaseRepository purchases;
    private final PaymentGateway gateway;

    public ShopService(UserRepository users, OwnedItemRepository owned, PurchaseRepository purchases,
            PaymentGateway gateway) {
        this.users = users;
        this.owned = owned;
        this.purchases = purchases;
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public List<ItemDto> catalog(Long userId) {
        User user = userId == null ? null : users.findById(userId).orElse(null);
        Set<String> mine = user == null ? Set.of()
                : owned.findByUserId(user.getId()).stream().map(OwnedItem::getItemId).collect(Collectors.toSet());
        String board = user == null ? ShopItem.BOARD_CLASSIC.id() : user.getBoardTheme();
        String pieces = user == null ? ShopItem.PIECES_CLASSIC.id() : user.getPieceTheme();
        return Arrays.stream(ShopItem.values())
                .map(i -> new ItemDto(i.id(), i.slot(), i.title(), i.description(), i.price(),
                        i.free() || mine.contains(i.id()),
                        i.id().equals(board) || i.id().equals(pieces)))
                .toList();
    }

    /** 결제를 시작한다. 결제창에는 여기서 받은 orderId와 금액을 그대로 넘긴다. */
    @Transactional
    public OrderDto order(long userId, String itemId) {
        ShopItem item = item(itemId);
        if (item.free() || owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new ShopException("이미 가지고 있는 상품입니다");
        }
        Purchase p = purchases.save(new Purchase(UUID.randomUUID().toString(), userId, item, gateway.name(),
                OffsetDateTime.now()));
        return new OrderDto(p.getOrderId(), item.id(), item.title(), p.getAmount(), p.getProvider());
    }

    /**
     * 결제사 승인을 받고 상품을 넣어 준다. 같은 주문을 두 번 확인해도 한 번만 들어간다.
     * 승인이 거절되면 주문을 실패로 남기고 ShopException을 던진다.
     */
    @Transactional(noRollbackFor = ShopException.class)
    public List<ItemDto> confirm(long userId, String orderId, String paymentKey) {
        Purchase p = purchases.findById(orderId)
                .filter(x -> x.getUserId() == userId)
                .orElseThrow(() -> new ShopException("주문을 찾을 수 없습니다"));
        if (p.getStatus() == Purchase.Status.FAILED) {
            throw new ShopException("실패한 주문입니다. 다시 주문해 주세요");
        }
        if (p.getStatus() == Purchase.Status.PENDING) {
            try {
                gateway.confirm(p.getOrderId(), paymentKey, p.getAmount());
            } catch (ShopException e) {
                p.failed();
                throw e;
            }
            p.paid(paymentKey, OffsetDateTime.now());
            if (!owned.existsByUserIdAndItemId(userId, p.getItemId())) {
                owned.save(new OwnedItem(userId, p.getItemId(), OffsetDateTime.now()));
            }
        }
        return catalog(userId);
    }

    @Transactional
    public List<ItemDto> equip(long userId, String itemId) {
        ShopItem item = item(itemId);
        if (!item.free() && !owned.existsByUserIdAndItemId(userId, item.id())) {
            throw new ShopException("먼저 상품을 사야 장착할 수 있습니다");
        }
        User user = users.findById(userId).orElseThrow(() -> new ShopException("회원을 찾을 수 없습니다"));
        switch (item.slot()) {
            case BOARD -> user.equipBoard(item.id());
            case PIECES -> user.equipPieces(item.id());
        }
        return catalog(userId);
    }

    private static ShopItem item(String id) {
        return ShopItem.byId(id).orElseThrow(() -> new ShopException("없는 상품입니다"));
    }
}
