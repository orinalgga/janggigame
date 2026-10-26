package com.janggi.server.game;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import com.janggi.server.chat.ChatService;
import com.janggi.server.record.GameRecord;
import com.janggi.server.record.GameRecorder;
import com.janggi.server.user.Grade;
import com.janggi.server.user.Look;
import com.janggi.server.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 진행 중인 대국은 메모리에 두고, 모든 수는 DB에 남긴다.
 * 서버가 다시 켜져 메모리에 없는 대국은 기보를 다시 두어 복원한다.
 * 온라인 대국은 바뀔 때마다 /topic/games/{id}로 두 사람에게 알린다.
 */
@Service
public class GameService {

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final GameRecorder recorder;
    private final UserService users;
    private final SimpMessagingTemplate messaging;
    private final ChatService chat;
    private final int baseSeconds;
    private final int incrementSeconds;

    public GameService(GameRecorder recorder, UserService users, SimpMessagingTemplate messaging, ChatService chat,
            @Value("${janggi.online.base-seconds}") int baseSeconds,
            @Value("${janggi.online.increment-seconds}") int incrementSeconds) {
        this.recorder = recorder;
        this.users = users;
        this.messaging = messaging;
        this.chat = chat;
        this.baseSeconds = baseSeconds;
        this.incrementSeconds = incrementSeconds;
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
        GameSession session = new GameSession(GameState.initial(cho, han), aiSide, aiLevel,
                GameSession.Players.NONE, null, 0, null, listener(id));
        sessions.put(id, session);
        if (aiSide != null) {
            chat.greet(id.toString(), aiSide, aiLevel);
        }
        session.start();
        return GameDto.from(id.toString(), session);
    }

    /** 매칭된 두 회원의 온라인 대국. 만들자마자 초의 시계가 간다. */
    public GameDto createOnline(long choUserId, Formation cho, long hanUserId, Formation han) {
        return createOnline(choUserId, cho, hanUserId, han, baseSeconds, incrementSeconds);
    }

    /** 시간 규칙을 정해 만드는 온라인 대국(대회). */
    public GameDto createOnline(long choUserId, Formation cho, long hanUserId, Formation han, int baseSeconds,
            int incrementSeconds) {
        UUID id = UUID.randomUUID();
        recorder.startOnline(id, cho, han, choUserId, hanUserId, baseSeconds, incrementSeconds);
        GameClock clock = new GameClock(baseSeconds * 1000L, baseSeconds * 1000L, incrementSeconds * 1000L);
        clock.start(Side.CHO, System.currentTimeMillis());
        GameSession session = new GameSession(GameState.initial(cho, han), null, null,
                players(choUserId, hanUserId, null), clock, 0, null, listener(id));
        sessions.put(id, session);
        return GameDto.from(id.toString(), session);
    }

    public GameDto get(String id) {
        return GameDto.from(id, find(id));
    }

    public GameDto play(String id, Move move, Long userId) {
        GameSession session = find(id);
        GameDto before = session.aiSide() == null ? null : GameDto.from(id, session);
        session.playHuman(move, userId);
        GameDto after = changed(id, session);
        // AI 대국이면 AI가 이번 수를 보고 한마디 할 수 있다.
        return before == null ? after : after.withAiComment(chat.commentOnMove(before, after));
    }

    public GameDto resign(String id, Long userId) {
        GameSession session = find(id);
        session.resign(userId);
        return changed(id, session);
    }

    /** 아무도 수를 두지 않아도 시간패가 나도록 진행 중인 온라인 대국 시계를 살핀다. */
    @Scheduled(fixedDelay = 250)
    void checkClocks() {
        sessions.forEach((id, session) -> {
            if (session.online() && session.checkTimeout()) {
                changed(id.toString(), session);
            }
        });
    }

    /** 서버가 꺼질 때 진행 중이던 온라인 대국을 되살려 시계가 다시 가게 한다. */
    @EventListener(ApplicationReadyEvent.class)
    void restoreOngoingOnline() {
        for (UUID id : recorder.ongoingOnline()) {
            sessions.computeIfAbsent(id, this::restore);
        }
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

    private GameDto changed(String id, GameSession session) {
        GameDto dto = GameDto.from(id, session);
        if (session.online()) {
            messaging.convertAndSend("/topic/games/" + id, dto);
        }
        return dto;
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
        RatingChange change = g.getChoRatingChange() == null ? null
                : new RatingChange(g.getChoRatingChange(), g.getHanRatingChange());
        GameClock clock = null;
        GameSession.Players players = GameSession.Players.NONE;
        if (g.isOnline()) {
            clock = new GameClock(g.getChoMsLeft(), g.getHanMsLeft(), g.getIncrementSeconds() * 1000L);
            // 마지막 수 이후로 흐른 시간(서버가 꺼져 있던 시간 포함)도 둘 차례인 쪽이 쓴 것으로 본다.
            clock.start(state.sideToMove(), g.getClockUpdatedAt().toInstant().toEpochMilli());
            players = players(g.getChoUserId(), g.getHanUserId(), change);
        }
        GameSession session = new GameSession(state, g.getAiSide(), g.getAiLevel(), players, clock,
                moves.size(), last, listener(id));
        if (g.getResult() != GameResult.ONGOING) {
            session.restoreEnd(g.getResult(), g.getEndReason(), change);
        }
        return session;
    }

    /** change가 있으면(끝난 대국) 대국 전 레이팅으로 되돌려 보여 준다. */
    private GameSession.Players players(long choUserId, long hanUserId, RatingChange change) {
        return new GameSession.Players(
                player(choUserId, change == null ? 0 : change.cho()),
                player(hanUserId, change == null ? 0 : change.han()));
    }

    private GameDto.PlayerDto player(long userId, int ratingChange) {
        return users.find(userId)
                .map(u -> new GameDto.PlayerDto(u.getId(), u.getNickname(), u.getRating() - ratingChange,
                        Grade.name(u.getRating() - ratingChange), Look.of(u)))
                .orElse(null);
    }

    private static GameState initial(GameRecord g) {
        return GameState.initial(g.getChoFormation(), g.getHanFormation());
    }

    private GameSession.Listener listener(UUID id) {
        return new GameSession.Listener() {
            @Override
            public void onMove(int seq, Move move, GameState after, GameClock.Snapshot clock) {
                recorder.recordMove(id, seq, move, clock == null ? null
                        : new GameRecorder.ClockState(clock.choMs(), clock.hanMs(), clock.at()));
            }

            @Override
            public RatingChange onEnd(GameResult result, String reason) {
                return recorder.finish(id, result, reason);
            }
        };
    }

    private static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new GameNotFoundException(id);
        }
    }
}
