-- 캐릭터 화풍(인형·애니메이션·조선 민화). 처음에는 모두 인형이다.
ALTER TABLE users ADD COLUMN style VARCHAR(40) NOT NULL DEFAULT 'style.doll';
