-- 관리자가 올리는 공지. 보이는 기간 안에서 켜 둔 것만 사용자에게 팝업으로 뜬다.
CREATE TABLE notices (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(60) NOT NULL,
    body       VARCHAR(2000) NOT NULL,
    starts_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at    TIMESTAMP WITH TIME ZONE,
    active     BOOLEAN NOT NULL,
    created_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX notices_starts_idx ON notices (starts_at DESC);

-- 주간·월간·연간 순위는 기간 안에 끝난 온라인 대국을 모은다.
CREATE INDEX games_ended_at_idx ON games (ended_at);
