-- 부품 그림을 겹친 2D 캐릭터(style.chibi)를 기본 화풍으로 한다. 기본이던 인형을 입고 있던 회원도 새 기본으로 옮긴다(인형은 무료라 언제든 다시 고를 수 있다).
ALTER TABLE users ALTER COLUMN style SET DEFAULT 'style.chibi';
UPDATE users SET style = 'style.chibi' WHERE style = 'style.doll';
