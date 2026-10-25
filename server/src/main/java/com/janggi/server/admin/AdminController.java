package com.janggi.server.admin;

import com.janggi.server.auth.CurrentUser;
import com.janggi.server.notice.NoticeService;
import com.janggi.server.user.NicknameException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 모든 요청은 먼저 관리자인지 확인한다. 로그인 자체는 SecurityConfig가 요구한다. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final NoticeService notices;

    public AdminController(AdminService admin, NoticeService notices) {
        this.admin = admin;
        this.notices = notices;
    }

    @GetMapping("/stats")
    public AdminService.StatsDto stats() {
        admin.requireAdmin(CurrentUser.id());
        return admin.stats();
    }

    @GetMapping("/users")
    public AdminService.UserPage users(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page) {
        admin.requireAdmin(CurrentUser.id());
        return admin.users(q, page);
    }

    @PutMapping("/users/{id}/nickname")
    public AdminService.UserRow rename(@PathVariable long id, @RequestBody Map<String, String> body) {
        admin.requireAdmin(CurrentUser.id());
        return admin.rename(CurrentUser.id(), id, body.get("nickname"));
    }

    @PostMapping("/users/{id}/ban")
    public AdminService.UserRow ban(@PathVariable long id, @RequestBody Map<String, String> body) {
        admin.requireAdmin(CurrentUser.id());
        return admin.ban(CurrentUser.id(), id, body.get("reason"));
    }

    @DeleteMapping("/users/{id}/ban")
    public AdminService.UserRow unban(@PathVariable long id) {
        admin.requireAdmin(CurrentUser.id());
        return admin.unban(CurrentUser.id(), id);
    }

    @PostMapping("/users/{id}/items")
    public AdminService.UserRow grant(@PathVariable long id, @RequestBody Map<String, String> body) {
        admin.requireAdmin(CurrentUser.id());
        return admin.grant(CurrentUser.id(), id, body.get("itemId"));
    }

    @GetMapping("/orders")
    public List<AdminService.OrderRow> orders() {
        admin.requireAdmin(CurrentUser.id());
        return admin.orders();
    }

    @GetMapping("/logs")
    public List<AdminService.LogRow> logs() {
        admin.requireAdmin(CurrentUser.id());
        return admin.logs();
    }

    @GetMapping("/notices")
    public List<NoticeService.NoticeDto> notices() {
        admin.requireAdmin(CurrentUser.id());
        return notices.all();
    }

    @PostMapping("/notices")
    public NoticeService.NoticeDto createNotice(@RequestBody NoticeService.NoticeRequest body) {
        admin.requireAdmin(CurrentUser.id());
        return admin.createNotice(CurrentUser.id(), body);
    }

    @PutMapping("/notices/{id}")
    public NoticeService.NoticeDto updateNotice(@PathVariable long id, @RequestBody NoticeService.NoticeRequest body) {
        admin.requireAdmin(CurrentUser.id());
        return admin.updateNotice(CurrentUser.id(), id, body);
    }

    @DeleteMapping("/notices/{id}")
    public Map<String, Boolean> deleteNotice(@PathVariable long id) {
        admin.requireAdmin(CurrentUser.id());
        admin.deleteNotice(CurrentUser.id(), id);
        return Map.of("deleted", true);
    }

    @ExceptionHandler(AdminException.Forbidden.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(AdminException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler({AdminException.BadRequest.class, NicknameException.class, NoticeService.InvalidNotice.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }
}
