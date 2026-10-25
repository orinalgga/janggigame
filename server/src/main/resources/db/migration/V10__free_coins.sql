-- 엽전을 충전한 것(paid_coins)과 운영자가 준 것(free_coins)으로 나눈다.
-- 운영자는 자기가 준 엽전만 회수할 수 있고, 살 때는 운영자가 준 엽전부터 쓴다.
ALTER TABLE users RENAME COLUMN coins TO paid_coins;
ALTER TABLE users DROP CONSTRAINT users_coins_nonneg;
ALTER TABLE users ADD COLUMN free_coins INTEGER NOT NULL DEFAULT 0;
-- 지금까지 운영자가 넣고 뺀 만큼(남은 잔액 안에서)을 운영자 엽전으로 옮긴다.
UPDATE users SET free_coins = LEAST(paid_coins, GREATEST(0, COALESCE(
    (SELECT SUM(l.delta) FROM coin_ledger l WHERE l.user_id = users.id AND l.kind = 'ADMIN'), 0)));
UPDATE users SET paid_coins = paid_coins - free_coins;
ALTER TABLE users ADD CONSTRAINT users_coins_nonneg CHECK (paid_coins >= 0 AND free_coins >= 0);

-- 한 번의 변동 중 운영자 엽전 몫. 나머지(delta - free_delta)가 충전 엽전 몫이다.
ALTER TABLE coin_ledger ADD COLUMN free_delta INTEGER NOT NULL DEFAULT 0;
UPDATE coin_ledger SET free_delta = delta WHERE kind = 'ADMIN';
