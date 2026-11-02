package com.janggi.server.match;

import com.janggi.core.Formation;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 온라인 대전 대기열. 같은 기본 시간을 고르고 먼저 기다리던 회원과 바로 짝짓고 진영은 무작위로 정한다.
 * 서버 한 대 기준이라 메모리에 둔다. 서버를 여러 대로 늘리면 Redis로 옮긴다.
 */
@Service
public class MatchService {

    public enum Status { WAITING, MATCHED, CANCELLED }

    /** MATCHED면 gameId가 있다. */
    public record MatchStatus(Status status, String gameId) {
    }

    /** 먼저 기다리던 회원은 STOMP로 /user/queue/match를 구독해 결과를 받는다. */
    public static final String DESTINATION = "/queue/match";

    private record Waiting(Formation formation, int baseSeconds) {
    }

    private final Map<Long, Waiting> waiting = new LinkedHashMap<>();
    private final GameService games;
    private final SimpMessagingTemplate messaging;
    private final Random random = new Random();

    public MatchService(GameService games, SimpMessagingTemplate messaging) {
        this.games = games;
        this.messaging = messaging;
    }

    public MatchStatus join(long userId, Formation formation, int baseSeconds) {
        Formation mine = formation == null ? Formation.MA_SANG_MA_SANG : formation;
        Long opponent;
        Formation theirs;
        synchronized (this) {
            // 다른 시간으로 기다리던 중이었다면 새로 고른 시간으로 다시 줄을 선다.
            waiting.remove(userId);
            opponent = firstOtherThan(userId, baseSeconds);
            if (opponent == null) {
                waiting.put(userId, new Waiting(mine, baseSeconds));
                return new MatchStatus(Status.WAITING, null);
            }
            theirs = waiting.remove(opponent).formation();
        }
        GameDto game = random.nextBoolean()
                ? games.createOnline(userId, mine, opponent, theirs, baseSeconds)
                : games.createOnline(opponent, theirs, userId, mine, baseSeconds);
        MatchStatus matched = new MatchStatus(Status.MATCHED, game.id());
        messaging.convertAndSendToUser(opponent.toString(), DESTINATION, matched);
        return matched;
    }

    /** 지금 상대를 기다리는 사람 수. */
    public synchronized int waitingCount() {
        return waiting.size();
    }

    public synchronized void leave(long userId) {
        waiting.remove(userId);
    }

    private Long firstOtherThan(long userId, int baseSeconds) {
        Iterator<Map.Entry<Long, Waiting>> it = waiting.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Waiting> e = it.next();
            if (e.getKey() != userId && e.getValue().baseSeconds() == baseSeconds) {
                return e.getKey();
            }
        }
        return null;
    }
}
