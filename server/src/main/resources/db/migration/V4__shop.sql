-- 상점: 장기판·장기알 테마. 상품 목록은 코드(ShopItem)에 있고, 여기에는 누가 무엇을 샀는지만 남긴다.
ALTER TABLE users ADD COLUMN board_theme VARCHAR(40) NOT NULL DEFAULT 'board.classic';
ALTER TABLE users ADD COLUMN piece_theme VARCHAR(40) NOT NULL DEFAULT 'pieces.classic';

CREATE TABLE user_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id     VARCHAR(40) NOT NULL,
    acquired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, item_id)
);

-- 주문 하나. 결제사가 승인하면 PAID가 되고 user_items에 상품이 들어간다.
CREATE TABLE purchases (
    order_id    VARCHAR(40) PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id     VARCHAR(40) NOT NULL,
    amount      INTEGER NOT NULL,
    provider    VARCHAR(20) NOT NULL,
    status      VARCHAR(10) NOT NULL,
    payment_key VARCHAR(200),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX purchases_user_idx ON purchases (user_id);
