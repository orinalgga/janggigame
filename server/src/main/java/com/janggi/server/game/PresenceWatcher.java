package com.janggi.server.game;

import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 온라인 대국 회원이 대국 화면(/topic/games/{id} 구독)에 있는지 1초마다 살핀다.
 * 창을 닫거나, 연결이 끊기거나, 대국 화면을 떠나면 구독이 사라진다.
 */
@Component
public class PresenceWatcher {

    private final SimpUserRegistry registry;
    private final GameService games;

    public PresenceWatcher(SimpUserRegistry registry, GameService games) {
        this.registry = registry;
        this.games = games;
    }

    @Scheduled(fixedDelay = 1000)
    void check() {
        games.checkPresence(this::watching);
    }

    /** 이 회원이 열어 둔 연결 가운데 하나라도 이 대국을 구독하고 있으면 true. */
    public boolean watching(String gameId, long userId) {
        SimpUser user = registry.getUser(String.valueOf(userId));
        if (user == null) {
            return false;
        }
        String topic = "/topic/games/" + gameId;
        return user.getSessions().stream()
                .flatMap(s -> s.getSubscriptions().stream())
                .anyMatch(sub -> topic.equals(sub.getDestination()));
    }
}
