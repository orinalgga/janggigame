-- 아바타: 상점에서 엽전으로 사거나, 운영 대회 우승으로만 얻는 보상 아바타가 있다. 목록은 코드(ShopItem)에 있다.
ALTER TABLE users ADD COLUMN avatar VARCHAR(40) NOT NULL DEFAULT 'avatar.basic';

-- 운영 대회 우승자에게 주는 보상 아바타(상점 상품 id). 개인 대회는 늘 비어 있다.
ALTER TABLE tournaments ADD COLUMN prize_item VARCHAR(40);
