package com.janggi.server.invite;

import com.janggi.core.Formation;
import com.janggi.server.auth.CurrentUser;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 초대 내용은 누구나 보고, 보내기·수락·거절은 로그인해야 한다(SecurityConfig). */
@RestController
@RequestMapping("/api/invites")
public class InviteController {

    /** nickname이 없으면 링크 초대를 만든다. formation은 내가 쓸 상차림. */
    public record InviteRequest(String nickname, Formation formation) {
    }

    public record AcceptRequest(Formation formation) {
    }

    private final InviteService invites;

    public InviteController(InviteService invites) {
        this.invites = invites;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InviteService.InviteDto create(@RequestBody InviteRequest request) {
        long me = CurrentUser.id();
        return request.nickname() == null
                ? invites.link(me, request.formation())
                : invites.invite(me, request.nickname(), request.formation());
    }

    @GetMapping("/{id}")
    public InviteService.InviteDto get(@PathVariable String id) {
        return invites.get(id);
    }

    @PostMapping("/{id}/accept")
    public InviteService.AcceptResult accept(@PathVariable String id,
            @RequestBody(required = false) AcceptRequest request) {
        return invites.accept(id, CurrentUser.id(), request == null ? null : request.formation());
    }

    @PostMapping("/{id}/decline")
    public Map<String, Boolean> decline(@PathVariable String id) {
        invites.decline(id, CurrentUser.id());
        return Map.of("declined", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> cancel(@PathVariable String id) {
        invites.cancel(id, CurrentUser.id());
        return Map.of("cancelled", true);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() != null) {
            try {
                invites.disconnected(Long.parseLong(event.getUser().getName()));
            } catch (NumberFormatException ignored) {
                // 회원 id가 아닌 주체는 초대를 보낼 수 없다.
            }
        }
    }

    @ExceptionHandler(InviteException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(InviteException e) {
        return Map.of("error", e.getMessage());
    }
}
