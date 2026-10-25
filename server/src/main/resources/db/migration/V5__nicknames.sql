-- 닉네임은 회원이 직접 정한다. 구글 이름이 그대로 보이지 않게 기존 회원은 임시 이름으로 바꾼다.
ALTER TABLE users ADD COLUMN nickname_chosen BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET nickname = '장기꾼' || id;
CREATE UNIQUE INDEX users_nickname_idx ON users (nickname);
