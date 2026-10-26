-- 확성기: 엽전으로 사서 모아 두는 소모품. 하나 쓰면 접속한 모두의 화면 위로 한 줄이 지나간다.
ALTER TABLE users ADD COLUMN megaphones INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD CONSTRAINT users_megaphones_nonneg CHECK (megaphones >= 0);

-- 쓴 확성기. 운영자가 내리면 hidden_at이 찬다.
CREATE TABLE megaphone_posts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text       VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    hidden_at  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX megaphone_posts_created_idx ON megaphone_posts (created_at DESC);
CREATE INDEX megaphone_posts_user_idx ON megaphone_posts (user_id, created_at DESC);
