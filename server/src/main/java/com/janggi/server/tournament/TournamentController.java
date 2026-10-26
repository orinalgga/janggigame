package com.janggi.server.tournament;

import com.janggi.server.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 대회 목록과 대진표는 누구나 보고(구경), 참가와 기권은 로그인해야 한다(SecurityConfig). */
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournaments;

    public TournamentController(TournamentService tournaments) {
        this.tournaments = tournaments;
    }

    @GetMapping
    public List<TournamentService.SummaryDto> list() {
        return tournaments.list(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public TournamentService.DetailDto detail(@PathVariable long id) {
        return tournaments.detail(id, CurrentUser.id());
    }

    /** 회원이 여는 개인 특별 리그. */
    @PostMapping
    public TournamentService.DetailDto host(@RequestBody TournamentService.TournamentRequest body) {
        return tournaments.host(CurrentUser.id(), body);
    }

    @PostMapping("/{id}/cancel")
    public TournamentService.DetailDto cancel(@PathVariable long id) {
        return tournaments.cancelHosted(id, CurrentUser.id());
    }

    @PostMapping("/{id}/join")
    public TournamentService.DetailDto join(@PathVariable long id) {
        return tournaments.join(id, CurrentUser.id());
    }

    @PostMapping("/{id}/leave")
    public TournamentService.DetailDto leave(@PathVariable long id) {
        return tournaments.leave(id, CurrentUser.id());
    }

    @ExceptionHandler(TournamentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(TournamentException e) {
        return Map.of("error", e.getMessage());
    }
}
