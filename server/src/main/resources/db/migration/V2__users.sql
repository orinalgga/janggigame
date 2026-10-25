CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    nickname   VARCHAR(40) NOT NULL,
    rating     INTEGER NOT NULL DEFAULT 1200,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 소셜 계정 연결. 한 사람이 여러 공급자(구글, 카카오, 애플)를 연결할 수 있다.
CREATE TABLE auth_identities (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider     VARCHAR(20) NOT NULL,
    provider_uid VARCHAR(128) NOT NULL,
    email        VARCHAR(255),
    UNIQUE (provider, provider_uid)
);

ALTER TABLE games ADD COLUMN cho_user_id BIGINT REFERENCES users (id);
ALTER TABLE games ADD COLUMN han_user_id BIGINT REFERENCES users (id);
CREATE INDEX games_cho_user_idx ON games (cho_user_id);
CREATE INDEX games_han_user_idx ON games (han_user_id);
