package com.janggi.server.shop;

import com.janggi.server.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 상품 목록은 누구나 보고, 사기와 장착은 로그인해야 한다(SecurityConfig). */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shop;

    public ShopController(ShopService shop) {
        this.shop = shop;
    }

    @GetMapping
    public ShopService.ShopDto shop() {
        return shop.shop(CurrentUser.id());
    }

    /** 엽전 충전 주문. */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopService.OrderDto order(@RequestBody OrderRequest request) {
        return shop.order(CurrentUser.id(), request.packId());
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ShopService.ShopDto confirm(@PathVariable String orderId, @RequestBody ConfirmRequest request) {
        return shop.confirm(CurrentUser.id(), orderId, request.paymentKey());
    }

    /** 엽전으로 테마 사기. */
    @PostMapping("/items/{itemId}/buy")
    public ShopService.ShopDto buy(@PathVariable String itemId) {
        return shop.buy(CurrentUser.id(), itemId);
    }

    @PutMapping("/equipped")
    public ShopService.ShopDto equip(@RequestBody ItemRequest request) {
        return shop.equip(CurrentUser.id(), request.itemId());
    }

    @GetMapping("/coins/history")
    public List<CoinService.LedgerRow> history() {
        return shop.history(CurrentUser.id());
    }

    public record OrderRequest(String packId) {
    }

    public record ItemRequest(String itemId) {
    }

    public record ConfirmRequest(String paymentKey) {
    }

    @ExceptionHandler(ShopException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(ShopException e) {
        return Map.of("error", e.getMessage());
    }
}
