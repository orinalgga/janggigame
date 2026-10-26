-- 대회(토너먼트): 정해진 시각에 대진표를 짜고, 지면 탈락한다. 한 라운드가 모두 끝나면 다음 라운드를 짝짓는다.
CREATE TABLE tournaments (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(60) NOT NULL,
    description       VARCHAR(1000),
    starts_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    base_seconds      INTEGER NOT NULL,
    increment_seconds INTEGER NOT NULL,
    entry_fee         INTEGER NOT NULL,
    max_players       INTEGER NOT NULL,
    -- 누가 나갈 수 있는지: ALL(누구나), DAN(초단 이상), KYU(급).
    division          VARCHAR(5) NOT NULL DEFAULT 'ALL',
    -- REGULAR(정기: repeat_days마다 다음 회차를 저절로 만든다) 또는 SPECIAL(특별).
    kind              VARCHAR(10) NOT NULL DEFAULT 'SPECIAL',
    repeat_days       INTEGER NOT NULL DEFAULT 0,
    -- 회원이 연 개인 대회면 연 사람. 운영자가 만든 대회는 비어 있다.
    host_id           BIGINT REFERENCES users (id) ON DELETE SET NULL,
    -- 참가비에 얹는 상금. 운영자 대회는 우승할 때 새로 주고, 개인 대회는 연 사람이 미리 낸다(낸 몫은 bonus_paid/bonus_free).
    bonus_prize       INTEGER NOT NULL DEFAULT 0,
    bonus_paid        INTEGER NOT NULL DEFAULT 0,
    bonus_free        INTEGER NOT NULL DEFAULT 0,
    status            VARCHAR(10) NOT NULL,
    current_round     INTEGER NOT NULL DEFAULT 0,
    -- 다음 라운드를 짝지을 때. 라운드가 모두 끝나면 잠깐 쉬었다가 시작한다.
    next_round_at     TIMESTAMP WITH TIME ZONE,
    winner_id         BIGINT REFERENCES users (id) ON DELETE SET NULL,
    prize             INTEGER NOT NULL DEFAULT 0,
    created_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX tournaments_starts_idx ON tournaments (starts_at DESC);

-- 참가자. 참가비를 충전 엽전(fee_paid)과 운영자 엽전(fee_free)에서 얼마씩 냈는지 남겨 환불할 때 그대로 돌려준다.
-- seed는 대회가 시작될 때 레이팅 순으로 정한다. eliminated_round는 진 라운드(우승자는 비어 있다).
CREATE TABLE tournament_entries (
    id               BIGSERIAL PRIMARY KEY,
    tournament_id    BIGINT NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seed             INTEGER,
    eliminated_round INTEGER,
    fee_paid         INTEGER NOT NULL DEFAULT 0,
    fee_free         INTEGER NOT NULL DEFAULT 0,
    joined_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (tournament_id, user_id)
);

-- 대진표의 한 칸. 라운드 r의 slot k는 앞 라운드 slot 2k, 2k+1의 승자끼리 둔다.
-- 한쪽이 비면 부전승이라 대국(game_id)이 없다.
CREATE TABLE tournament_matches (
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    round         INTEGER NOT NULL,
    slot          INTEGER NOT NULL,
    cho_user_id   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    han_user_id   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    game_id       UUID REFERENCES games (id) ON DELETE SET NULL,
    winner_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (tournament_id, round, slot)
);
