-- 캐릭터를 미니미로 바꾸면서 기본 차림을 요즘 옷으로 바꾼다.
-- 무료 한복 기본(상투, 저고리)을 입고 있던 회원만 새 기본으로 옮기고, 산 물건은 그대로 둔다.
ALTER TABLE users ALTER COLUMN hair SET DEFAULT 'hair.short';
ALTER TABLE users ALTER COLUMN outfit SET DEFAULT 'outfit.tee';

UPDATE users SET hair = 'hair.bob' WHERE hair = 'hair.jjok' AND outfit = 'outfit.plain';
UPDATE users SET hair = 'hair.short' WHERE hair = 'hair.sangtu' AND outfit = 'outfit.plain';
UPDATE users SET outfit = 'outfit.tee' WHERE outfit = 'outfit.plain';
