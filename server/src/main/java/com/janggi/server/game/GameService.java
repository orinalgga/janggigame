package com.janggi.server.game;

import com.janggi.core.Formation;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 메모리에만 두는 대국 저장소. 0단계에서 규칙 엔진을 웹과 연결해 보기 위한 것이고,
 * 1단계에서 DB 저장과 사용자 연결로 바꾼다.
 */
@Service
public class GameService {

    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    public GameDto create(Formation cho, Formation han) {
        String id = UUID.randomUUID().toString();
        GameState state = GameState.initial(cho, han);
        games.put(id, state);
        return GameDto.from(id, state);
    }

    public GameDto get(String id) {
        return GameDto.from(id, find(id));
    }

    public GameDto play(String id, Move move) {
        GameState next = games.compute(id, (key, state) -> {
            if (state == null) {
                throw new GameNotFoundException(key);
            }
            return state.apply(move);
        });
        return GameDto.from(id, next);
    }

    private GameState find(String id) {
        GameState state = games.get(id);
        if (state == null) {
            throw new GameNotFoundException(id);
        }
        return state;
    }
}
