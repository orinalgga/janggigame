-- 온라인 대국: 시간 규칙, 남은 시간, 레이팅 변화.
ALTER TABLE games ADD COLUMN online BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE games ADD COLUMN base_seconds INTEGER;
ALTER TABLE games ADD COLUMN increment_seconds INTEGER;
-- 마지막 수를 둔 시점(clock_updated_at)의 남은 시간. 서버가 다시 켜지면 이것으로 시계를 복원한다.
ALTER TABLE games ADD COLUMN cho_ms_left BIGINT;
ALTER TABLE games ADD COLUMN han_ms_left BIGINT;
ALTER TABLE games ADD COLUMN clock_updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE games ADD COLUMN cho_rating_change INTEGER;
ALTER TABLE games ADD COLUMN han_rating_change INTEGER;

CREATE INDEX games_online_ongoing_idx ON games (online, result);
