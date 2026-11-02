package com.janggi.server.presence;

import com.janggi.server.game.GameService;
import com.janggi.server.user.Grade;
import com.janggi.server.user.Look;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지금 접속해 있는 회원 목록. 로그인한 회원은 어느 화면에 있든 초대 알림용 STOMP 연결을 열어 두므로
 * 그 연결이 있는 회원을 '접속 중'으로 본다. 로비에서 골라 바로 초대할 수 있게 한다.
 */
@RestController
@RequestMapping("/api/online")
public class OnlineController {

    /** 너무 길어지지 않게 이만큼만 보여 준다. */
    static final int LIMIT = 100;

    /** playing이면 지금 온라인 대국을 두는 중이라 초대할 수 없다. */
    public record OnlineUser(long id, String nickname, int rating, String grade, Look look, boolean playing) {
    }

    public record OnlineDto(int count, List<OnlineUser> users) {
    }

    private final SimpUserRegistry registry;
    private final UserRepository users;
    private final GameService games;

    public OnlineController(SimpUserRegistry registry, UserRepository users, GameService games) {
        this.registry = registry;
        this.users = users;
        this.games = games;
    }

    @GetMapping
    public OnlineDto online() {
        List<Long> ids = new ArrayList<>();
        for (SimpUser u : registry.getUsers()) {
            try {
                ids.add(Long.parseLong(u.getName()));
            } catch (NumberFormatException ignored) {
                // 회원 id가 아닌 주체(비회원)는 목록에 넣지 않는다.
            }
        }
        Set<Long> playing = games.playingOnline();
        List<OnlineUser> list = users.findAllById(ids).stream()
                .filter(u -> !u.isBanned())
                .map(u -> of(u, playing.contains(u.getId())))
                // 바로 둘 수 있는 사람부터, 그 안에서는 레이팅 높은 순.
                .sorted(Comparator.comparing(OnlineUser::playing)
                        .thenComparing(OnlineUser::rating, Comparator.reverseOrder()))
                .toList();
        return new OnlineDto(list.size(), list.size() > LIMIT ? list.subList(0, LIMIT) : list);
    }

    private static OnlineUser of(User u, boolean playing) {
        return new OnlineUser(u.getId(), u.getNickname(), u.getRating(), Grade.name(u.getRating()), Look.of(u),
                playing);
    }
}
