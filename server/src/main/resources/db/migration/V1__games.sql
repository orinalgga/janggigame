CREATE TABLE games (
    id            UUID PRIMARY KEY,
    cho_formation VARCHAR(20) NOT NULL,
    han_formation VARCHAR(20) NOT NULL,
    ai_side       VARCHAR(3),
    ai_level      VARCHAR(12),
    result        VARCHAR(10) NOT NULL,
    end_reason    VARCHAR(10),
    move_count    INTEGER NOT NULL DEFAULT 0,
    started_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX games_started_at_idx ON games (started_at DESC);

-- 한수쉼은 from/to가 모두 NULL이다.
CREATE TABLE game_moves (
    game_id UUID     NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    seq     INTEGER  NOT NULL,
    from_x  SMALLINT,
    from_y  SMALLINT,
    to_x    SMALLINT,
    to_y    SMALLINT,
    PRIMARY KEY (game_id, seq)
);
