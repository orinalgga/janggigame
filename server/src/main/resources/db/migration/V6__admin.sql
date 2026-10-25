-- 관리자가 이용을 정지한 회원. banned_at이 비어 있으면 정상.
ALTER TABLE users ADD COLUMN banned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN ban_reason VARCHAR(200);
CREATE INDEX purchases_created_idx ON purchases (created_at);
