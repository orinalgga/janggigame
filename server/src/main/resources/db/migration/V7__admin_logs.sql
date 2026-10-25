-- 관리자가 한 일. 누가 언제 누구에게 무엇을 했는지 남긴다.
CREATE TABLE admin_logs (
    id             BIGSERIAL PRIMARY KEY,
    admin_id       BIGINT NOT NULL REFERENCES users (id),
    action         VARCHAR(20) NOT NULL,
    target_user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    detail         VARCHAR(300),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX admin_logs_created_idx ON admin_logs (created_at DESC);
