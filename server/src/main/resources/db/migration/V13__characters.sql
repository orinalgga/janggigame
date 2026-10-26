-- 캐릭터 아바타: 몸(사람·동물), 머리 모양, 옷, 모자를 따로 장착한다. users.avatar는 배경 패가 된다.
ALTER TABLE users ADD COLUMN body VARCHAR(40) NOT NULL DEFAULT 'body.human';
ALTER TABLE users ADD COLUMN hair VARCHAR(40) NOT NULL DEFAULT 'hair.bob';
ALTER TABLE users ADD COLUMN outfit VARCHAR(40) NOT NULL DEFAULT 'outfit.white';
ALTER TABLE users ADD COLUMN hat VARCHAR(40) NOT NULL DEFAULT 'hat.none';
