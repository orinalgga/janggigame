package com.janggi.server.megaphone;

import com.janggi.server.auth.CurrentUser;
import com.janggi.server.shop.ShopException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 최근 확성기는 누구나 보고, 사기와 쓰기는 로그인해야 한다(SecurityConfig). */
@RestController
@RequestMapping("/api/megaphone")
public class MegaphoneController {

    private final MegaphoneService megaphone;

    public MegaphoneController(MegaphoneService megaphone) {
        this.megaphone = megaphone;
    }

    @GetMapping
    public MegaphoneService.StateDto state() {
        return megaphone.state(CurrentUser.id());
    }

    @PostMapping("/buy")
    public MegaphoneService.StateDto buy(@RequestBody BuyRequest request) {
        return megaphone.buy(CurrentUser.id(), request.packId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MegaphoneService.PostDto post(@RequestBody PostRequest request) {
        return megaphone.post(CurrentUser.id(), request.text());
    }

    public record BuyRequest(String packId) {
    }

    public record PostRequest(String text) {
    }

    @ExceptionHandler({MegaphoneException.class, ShopException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }
}
