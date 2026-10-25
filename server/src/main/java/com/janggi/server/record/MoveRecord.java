package com.janggi.server.record;

import com.janggi.core.Move;
import com.janggi.core.Square;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/** 기보의 한 수. 한수쉼은 좌표가 모두 null. */
@Entity
@Table(name = "game_moves")
@IdClass(MoveRecord.Key.class)
public class MoveRecord {

    public record Key(UUID gameId, int seq) implements Serializable {
        public Key() {
            this(null, 0);
        }
    }

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Id
    private int seq;

    @Column(name = "from_x")
    private Short fromX;

    @Column(name = "from_y")
    private Short fromY;

    @Column(name = "to_x")
    private Short toX;

    @Column(name = "to_y")
    private Short toY;

    protected MoveRecord() {
    }

    public MoveRecord(UUID gameId, int seq, Move move) {
        this.gameId = gameId;
        this.seq = seq;
        if (!move.isPass()) {
            fromX = (short) move.from().x();
            fromY = (short) move.from().y();
            toX = (short) move.to().x();
            toY = (short) move.to().y();
        }
    }

    public int getSeq() {
        return seq;
    }

    public Move toMove() {
        if (fromX == null) {
            return Move.pass();
        }
        return new Move(Square.of(fromX, fromY), Square.of(toX, toY));
    }
}
