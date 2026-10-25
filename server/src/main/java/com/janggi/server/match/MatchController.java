package com.janggi.server.match;

import com.janggi.core.Formation;
import com.janggi.server.auth.CurrentUser;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 로그인한 회원만 쓸 수 있다(SecurityConfig). */
@RestController
@RequestMapping("/api/match")
public class MatchController {

    /** formation은 내가 쓸 상차림. 초가 되든 한이 되든 그대로 쓴다. */
    public record MatchRequest(Formation formation) {
    }

    private final MatchService matches;

    public MatchController(MatchService matches) {
        this.matches = matches;
    }

    @PostMapping
    public MatchService.MatchStatus join(@RequestBody(required = false) MatchRequest request) {
        return matches.join(CurrentUser.id(), request == null ? null : request.formation());
    }

    @DeleteMapping
    public MatchService.MatchStatus leave() {
        matches.leave(CurrentUser.id());
        return new MatchService.MatchStatus(MatchService.Status.CANCELLED, null);
    }

    /** 창을 닫거나 연결이 끊긴 회원은 대기열에서 뺀다. 없는 사람과 짝지어지지 않도록. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() != null) {
            try {
                matches.leave(Long.parseLong(event.getUser().getName()));
            } catch (NumberFormatException ignored) {
                // 회원 id가 아닌 주체는 대기열에 없다.
            }
        }
    }
}
