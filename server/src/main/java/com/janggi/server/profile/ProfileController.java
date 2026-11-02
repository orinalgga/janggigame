package com.janggi.server.profile;

import com.janggi.core.Side;
import com.janggi.server.game.GameSummaryDto;
import com.janggi.server.record.GameRecord;
import com.janggi.server.record.GameRecordRepository;
import com.janggi.server.user.Grade;
import com.janggi.server.user.Look;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 다른 회원의 프로필과 온라인 대국 전적. 레이팅이 걸린 온라인 대국만 센다. 누구나 볼 수 있다. */
@RestController
@RequestMapping("/api/users")
public class ProfileController {

    /** recent는 그 회원 쪽에서 본 최근 온라인 대국(최대 10판). */
    public record ProfileDto(long id, String nickname, int rating, String grade, Look look, long wins, long losses,
            long draws, List<GameSummaryDto> recent) {
    }

    static class NoSuchUser extends RuntimeException {
    }

    private final UserRepository users;
    private final GameRecordRepository games;

    public ProfileController(UserRepository users, GameRecordRepository games) {
        this.users = users;
        this.games = games;
    }

    @GetMapping("/{id}/profile")
    @Transactional(readOnly = true)
    public ProfileDto profile(@PathVariable long id) {
        User u = users.findById(id).filter(x -> !x.isBanned()).orElseThrow(NoSuchUser::new);
        Object[] r = games.onlineRecordOf(id).get(0);
        List<GameSummaryDto> recent = games
                .findTop10ByOnlineTrueAndChoUserIdOrOnlineTrueAndHanUserIdOrderByStartedAtDesc(id, id).stream()
                .map(g -> GameSummaryDto.from(g, id, opponentNickname(g, id)))
                .toList();
        return new ProfileDto(u.getId(), u.getNickname(), u.getRating(), Grade.name(u.getRating()), Look.of(u),
                number(r[0]), number(r[1]), number(r[2]), recent);
    }

    private String opponentNickname(GameRecord g, long id) {
        Side mine = GameSummaryDto.sideOf(g, id);
        Long other = mine == null ? null : mine == Side.CHO ? g.getHanUserId() : g.getChoUserId();
        return other == null ? null : users.findById(other).map(User::getNickname).orElse(null);
    }

    private static long number(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    @ExceptionHandler(NoSuchUser.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound() {
        return Map.of("error", "회원을 찾을 수 없어요");
    }
}
