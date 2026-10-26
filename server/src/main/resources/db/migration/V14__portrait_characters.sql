-- 캐릭터를 조선 초상화풍으로 바꾼다: 수염 칸을 더하고, 없어진 부품은 비슷한 새 부품으로 옮기거나 기본으로 되돌린다.
ALTER TABLE users ADD COLUMN beard VARCHAR(40) NOT NULL DEFAULT 'beard.none';
ALTER TABLE users ALTER COLUMN body SET DEFAULT 'body.man';
ALTER TABLE users ALTER COLUMN hair SET DEFAULT 'hair.sangtu';
ALTER TABLE users ALTER COLUMN outfit SET DEFAULT 'outfit.plain';

-- 산 부품 중 뜻이 이어지는 것은 새 이름으로 옮긴다(새 부품이라 겹칠 일이 없다).
UPDATE user_items SET item_id = 'outfit.durumagi' WHERE item_id = 'outfit.saekdong';
UPDATE user_items SET item_id = 'outfit.dangui' WHERE item_id = 'outfit.pink';
UPDATE user_items SET item_id = 'outfit.wonsam' WHERE item_id = 'outfit.hwarot';
UPDATE user_items SET item_id = 'outfit.cheollik' WHERE item_id = 'outfit.black';
UPDATE user_items SET item_id = 'hat.jeonrip' WHERE item_id = 'hat.helmet';
UPDATE user_items SET item_id = 'hat.binyeo' WHERE item_id = 'hat.flower';
UPDATE user_items SET item_id = 'hat.tteoljam' WHERE item_id = 'hat.hwagwan';
UPDATE user_items SET item_id = 'hair.gache' WHERE item_id = 'hair.long';
UPDATE user_items SET item_id = 'hair.loose' WHERE item_id = 'hair.messy';
-- 동물 몸과 아이 머리는 없어졌다. 출시 전이라 산 사람이 없다.
DELETE FROM user_items WHERE item_id IN ('body.rabbit', 'body.cat', 'body.bear', 'body.fox', 'body.tiger', 'body.dragon',
    'hair.daenggi', 'hair.twin', 'hair.silver');

UPDATE users SET outfit = CASE outfit
    WHEN 'outfit.saekdong' THEN 'outfit.durumagi' WHEN 'outfit.pink' THEN 'outfit.dangui'
    WHEN 'outfit.hwarot' THEN 'outfit.wonsam' WHEN 'outfit.black' THEN 'outfit.cheollik'
    WHEN 'outfit.white' THEN 'outfit.plain' ELSE outfit END;
UPDATE users SET hat = CASE hat
    WHEN 'hat.helmet' THEN 'hat.jeonrip' WHEN 'hat.flower' THEN 'hat.binyeo' WHEN 'hat.hwagwan' THEN 'hat.tteoljam'
    ELSE hat END;
UPDATE users SET hair = CASE hair
    WHEN 'hair.long' THEN 'hair.gache' WHEN 'hair.messy' THEN 'hair.loose'
    WHEN 'hair.daenggi' THEN 'hair.jjok' WHEN 'hair.twin' THEN 'hair.jjok' WHEN 'hair.jjok' THEN 'hair.jjok'
    WHEN 'hair.gache' THEN 'hair.gache' WHEN 'hair.loose' THEN 'hair.loose' ELSE 'hair.sangtu' END;
UPDATE users SET body = CASE body
    WHEN 'body.man' THEN 'body.man' WHEN 'body.woman' THEN 'body.woman'
    WHEN 'body.elder' THEN 'body.elder' WHEN 'body.elder.woman' THEN 'body.elder.woman' ELSE 'body.man' END;
