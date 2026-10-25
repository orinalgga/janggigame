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
 * 온라인 대전 대기열. 먼저 기다리던 회원과 바로 짝짓고 진영은 무작위로 정한다.
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

    private final Map<Long, Formation> waiting = new LinkedHashMap<>();
    private final GameService games;
    private final SimpMessagingTemplate messaging;
    private final Random random = new Random();

    public MatchService(GameService games, SimpMessagingTemplate messaging) {
        this.games = games;
        this.messaging = messaging;
    }

    public MatchStatus join(long userId, Formation formation) {
        Formation mine = formation == null ? Formation.MA_SANG_MA_SANG : formation;
        Long opponent;
        Formation theirs;
        synchronized (this) {
            opponent = firstOtherThan(userId);
            if (opponent == null) {
                waiting.put(userId, mine);
                return new MatchStatus(Status.WAITING, null);
            }
            theirs = waiting.remove(opponent);
        }
        GameDto game = random.nextBoolean()
                ? games.createOnline(userId, mine, opponent, theirs)
                : games.createOnline(opponent, theirs, userId, mine);
        MatchStatus matched = new MatchStatus(Status.MATCHED, game.id());
        messaging.convertAndSendToUser(opponent.toString(), DESTINATION, matched);
        return matched;
    }

    public synchronized void leave(long userId) {
        waiting.remove(userId);
    }

    private Long firstOtherThan(long userId) {
        Iterator<Long> it = waiting.keySet().iterator();
        while (it.hasNext()) {
            Long id = it.next();
            if (id != userId) {
                return id;
            }
        }
        return null;
    }
}
