-- 장기 화폐 '엽전'. 100원에 1닢으로 충전하고, 테마는 엽전으로 산다.
-- 잔액은 JPA 엔티티에 두지 않고 CoinService가 한 줄 UPDATE로만 바꾼다(동시에 써도 어긋나지 않게).
ALTER TABLE users ADD COLUMN coins INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD CONSTRAINT users_coins_nonneg CHECK (coins >= 0);

-- 엽전이 들고 난 기록. balance는 바뀐 뒤의 잔액.
CREATE TABLE coin_ledger (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    delta      INTEGER NOT NULL,
    balance    INTEGER NOT NULL,
    kind       VARCHAR(10) NOT NULL,
    ref        VARCHAR(60),
    memo       VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX coin_ledger_user_idx ON coin_ledger (user_id, id DESC);
