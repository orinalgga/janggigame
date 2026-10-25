package com.janggi.server.ranking;

import com.janggi.server.auth.CurrentUser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 순위는 누구나 본다. 로그인했으면 내 줄도 함께 준다. */
@RestController
public class RankingController {

    private final RankingService rankings;

    public RankingController(RankingService rankings) {
        this.rankings = rankings;
    }

    @GetMapping("/api/rankings")
    public RankingService.RankingDto rankings(@RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "0") int offset) {
        return rankings.rankings(RankingService.Period.parse(period), offset, CurrentUser.id());
    }

    @ExceptionHandler(RankingService.BadPeriod.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> bad(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }
}
