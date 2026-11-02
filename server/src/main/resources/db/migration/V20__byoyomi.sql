-- 온라인 대국 시간 규칙을 증초(피셔)에서 초읽기로 바꾼다.
-- increment_seconds는 옛 대국 기록으로만 남는다.
ALTER TABLE games ADD COLUMN byoyomi_seconds INTEGER;
ALTER TABLE games ADD COLUMN byoyomi_periods INTEGER;
-- 마지막 수를 둔 시점의 남은 초읽기 횟수.
ALTER TABLE games ADD COLUMN cho_periods_left INTEGER;
ALTER TABLE games ADD COLUMN han_periods_left INTEGER;
