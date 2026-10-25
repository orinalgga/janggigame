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
    public List<ShopService.ItemDto> catalog() {
        return shop.catalog(CurrentUser.id());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopService.OrderDto order(@RequestBody OrderRequest request) {
        return shop.order(CurrentUser.id(), request.itemId());
    }

    @PostMapping("/orders/{orderId}/confirm")
    public List<ShopService.ItemDto> confirm(@PathVariable String orderId, @RequestBody ConfirmRequest request) {
        return shop.confirm(CurrentUser.id(), orderId, request.paymentKey());
    }

    @PutMapping("/equipped")
    public List<ShopService.ItemDto> equip(@RequestBody OrderRequest request) {
        return shop.equip(CurrentUser.id(), request.itemId());
    }

    public record OrderRequest(String itemId) {
    }

    public record ConfirmRequest(String paymentKey) {
    }

    @ExceptionHandler(ShopException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(ShopException e) {
        return Map.of("error", e.getMessage());
    }
}
