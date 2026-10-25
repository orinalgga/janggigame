package com.janggi.server.record;

import com.janggi.core.Engine;
import com.janggi.core.Formation;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Side;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 대국과 수를 DB에 남긴다. */
@Service
public class GameRecorder {

    public record Loaded(GameRecord game, List<Move> moves) {
    }

    private final GameRecordRepository games;
    private final MoveRecordRepository moves;

    public GameRecorder(GameRecordRepository games, MoveRecordRepository moves) {
        this.games = games;
        this.moves = moves;
    }

    @Transactional
    public void start(UUID id, Formation cho, Formation han, Side aiSide, Engine.Level aiLevel,
            Long choUserId, Long hanUserId) {
        games.save(new GameRecord(id, cho, han, aiSide, aiLevel, choUserId, hanUserId, OffsetDateTime.now()));
    }

    /** seq는 1부터 센 수 번호. after는 그 수를 둔 뒤의 국면. */
    @Transactional
    public void recordMove(UUID id, int seq, Move move, GameState after) {
        moves.save(new MoveRecord(id, seq, move));
        GameRecord game = games.findById(id).orElseThrow();
        game.moved(seq);
        if (after.isGameOver()) {
            game.finish(after.result(), after.isCheckmate() ? "CHECKMATE" : "BIKJANG", OffsetDateTime.now());
        }
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
}
