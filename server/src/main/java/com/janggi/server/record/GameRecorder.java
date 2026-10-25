package com.janggi.server.record;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.core.Move;
import com.janggi.core.Side;
import com.janggi.server.game.RatingChange;
import com.janggi.server.user.Elo;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 대국과 수를 DB에 남기고, 온라인 대국이 끝나면 레이팅을 반영한다. */
@Service
public class GameRecorder {

    public record Loaded(GameRecord game, List<Move> moves) {
    }

    /** 수를 둔 직후의 남은 시간. at은 그 시각(epoch 밀리초). */
    public record ClockState(long choMs, long hanMs, long at) {
    }

    private final GameRecordRepository games;
    private final MoveRecordRepository moves;
    private final UserRepository users;

    public GameRecorder(GameRecordRepository games, MoveRecordRepository moves, UserRepository users) {
        this.games = games;
        this.moves = moves;
        this.users = users;
    }

    @Transactional
    public void start(UUID id, Formation cho, Formation han, Side aiSide, Engine.Level aiLevel,
            Long choUserId, Long hanUserId) {
        games.save(new GameRecord(id, cho, han, aiSide, aiLevel, choUserId, hanUserId, OffsetDateTime.now()));
    }

    @Transactional
    public void startOnline(UUID id, Formation cho, Formation han, long choUserId, long hanUserId,
            int baseSeconds, int incrementSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        GameRecord game = new GameRecord(id, cho, han, null, null, choUserId, hanUserId, now);
        game.startClock(baseSeconds, incrementSeconds, now);
        games.save(game);
    }

    /** seq는 1부터 센 수 번호. clock은 온라인 대국에서만 있다. */
    @Transactional
    public void recordMove(UUID id, int seq, Move move, ClockState clock) {
        moves.save(new MoveRecord(id, seq, move));
        GameRecord game = games.findById(id).orElseThrow();
        game.moved(seq);
        if (clock != null) {
            game.clock(clock.choMs(), clock.hanMs(), Instant.ofEpochMilli(clock.at()).atOffset(ZoneOffset.UTC));
        }
    }

    /** 대국을 끝낸다. 두 쪽 모두 회원인 온라인 대국이면 레이팅을 바꾸고 그 양을 돌려준다. */
    @Transactional
    public RatingChange finish(UUID id, GameResult result, String reason) {
        GameRecord game = games.findById(id).orElseThrow();
        game.finish(result, reason, OffsetDateTime.now());
        if (!game.isOnline() || game.getChoUserId() == null || game.getHanUserId() == null) {
            return null;
        }
        User cho = users.findById(game.getChoUserId()).orElseThrow();
        User han = users.findById(game.getHanUserId()).orElseThrow();
        double choScore = switch (result) {
            case CHO_WIN -> 1.0;
            case HAN_WIN -> 0.0;
            default -> 0.5;
        };
        int choDelta = Elo.change(cho.getRating(), han.getRating(), choScore);
        int hanDelta = Elo.change(han.getRating(), cho.getRating(), 1.0 - choScore);
        cho.changeRating(choDelta);
        han.changeRating(hanDelta);
        game.ratingChanged(choDelta, hanDelta);
        return new RatingChange(choDelta, hanDelta);
    }

    @Transactional(readOnly = true)
    public Optional<Loaded> load(UUID id) {
        return games.findById(id).map(g -> new Loaded(
                g, moves.findByGameIdOrderBySeq(id).stream().map(MoveRecord::toMove).toList()));
    }

    @Transactional(readOnly = true)
    public List<GameRecord> recent() {
        return games.findTop20ByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<GameRecord> recentOf(long userId) {
        return games.findTop20ByChoUserIdOrHanUserIdOrderByStartedAtDesc(userId, userId);
    }

    /** 서버가 꺼질 때 진행 중이던 온라인 대국. */
    @Transactional(readOnly = true)
    public List<UUID> ongoingOnline() {
        return games.findByOnlineTrueAndResult(GameResult.ONGOING).stream().map(GameRecord::getId).toList();
    }
}
