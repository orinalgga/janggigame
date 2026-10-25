package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 메모리에만 두는 대국 저장소. 기보 저장 단계에서 DB 저장으로 바꾼다.
 */
@Service
public class GameService {

    private final Map<String, GameSession> games = new ConcurrentHashMap<>();

    /** aiSide가 null이면 한 화면에서 둘이 두는 대국. */
    public GameDto create(Formation cho, Formation han, Side aiSide, Engine.Level aiLevel) {
        String id = UUID.randomUUID().toString();
        GameSession session = new GameSession(GameState.initial(cho, han), aiSide, aiLevel);
        games.put(id, session);
        return GameDto.from(id, session);
    }

    public GameDto get(String id) {
        return GameDto.from(id, find(id));
    }

    public GameDto play(String id, Move move) {
        GameSession session = find(id);
        session.playHuman(move);
        return GameDto.from(id, session);
    }

    private GameSession find(String id) {
        GameSession session = games.get(id);
        if (session == null) {
            throw new GameNotFoundException(id);
        }
        return session;
    }
}
