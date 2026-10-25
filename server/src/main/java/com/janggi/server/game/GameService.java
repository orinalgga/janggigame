package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import com.janggi.server.record.GameRecord;
import com.janggi.server.record.GameRecorder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 진행 중인 대국은 메모리에 두고, 모든 수는 DB에 남긴다.
 * 서버가 다시 켜져 메모리에 없는 대국은 기보를 다시 두어 복원한다.
 */
@Service
public class GameService {

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final GameRecorder recorder;

    public GameService(GameRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * aiSide가 null이면 한 화면에서 둘이 두는 대국. userId가 있으면 사람이 두는 쪽에 회원을 기록한다
     * (2인 대국이면 두 쪽 모두 같은 회원).
     */
    public GameDto create(Formation cho, Formation han, Side aiSide, Engine.Level aiLevel, Long userId) {
        UUID id = UUID.randomUUID();
        Long choUser = aiSide == Side.CHO ? null : userId;
        Long hanUser = aiSide == Side.HAN ? null : userId;
        recorder.start(id, cho, han, aiSide, aiLevel, choUser, hanUser);
        GameSession session = new GameSession(GameState.initial(cho, han), aiSide, aiLevel, 0, null, listener(id));
        sessions.put(id, session);
        session.start();
        return GameDto.from(id.toString(), session);
    }

    public GameDto get(String id) {
        return GameDto.from(id, find(id));
    }

    public GameDto play(String id, Move move) {
        GameSession session = find(id);
        session.playHuman(move);
        return GameDto.from(id, session);
    }

    /** ply번째 수까지 둔 국면. 0이면 첫 배치. */
    public ReplayDto replay(String id, int ply) {
        GameRecorder.Loaded loaded = recorder.load(parse(id)).orElseThrow(() -> new GameNotFoundException(id));
        List<Move> moves = loaded.moves();
        int upTo = Math.max(0, Math.min(ply, moves.size()));
        GameState state = initial(loaded.game());
        for (int i = 0; i < upTo; i++) {
            state = state.apply(moves.get(i));
        }
        GameDto.MoveDto last = upTo == 0 ? null : GameDto.MoveDto.from(moves.get(upTo - 1));
        return new ReplayDto(id, upTo, moves.size(), GameDto.pieces(state), last, state.isCheck(),
                loaded.game().getResult(), loaded.game().getAiSide());
    }

    /** userId가 있으면 그 회원의 대국, 없으면 전체 최근 대국. */
    public List<GameSummaryDto> recent(Long userId) {
        var records = userId == null ? recorder.recent() : recorder.recentOf(userId);
        return records.stream().map(GameSummaryDto::from).toList();
    }

    private GameSession find(String id) {
        GameSession session = sessions.computeIfAbsent(parse(id), this::restore);
        // 복원한 대국이 AI 차례에서 멈춰 있었다면 이어서 둔다. AI 차례가 아니면 아무 일도 없다.
        session.start();
        return session;
    }

    private GameSession restore(UUID id) {
        GameRecorder.Loaded loaded = recorder.load(id).orElseThrow(() -> new GameNotFoundException(id.toString()));
        GameState state = initial(loaded.game());
        for (Move m : loaded.moves()) {
            state = state.apply(m);
        }
        List<Move> moves = loaded.moves();
        Move last = moves.isEmpty() ? null : moves.get(moves.size() - 1);
        GameRecord g = loaded.game();
        return new GameSession(state, g.getAiSide(), g.getAiLevel(), moves.size(), last, listener(id));
    }

    private static GameState initial(GameRecord g) {
        return GameState.initial(g.getChoFormation(), g.getHanFormation());
    }

    private GameSession.MoveListener listener(UUID id) {
        return (seq, move, after) -> recorder.recordMove(id, seq, move, after);
    }

    private static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new GameNotFoundException(id);
        }
    }
}
