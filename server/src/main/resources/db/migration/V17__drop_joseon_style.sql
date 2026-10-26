-- 조선 민화 화풍을 내린다. 쓰던 회원은 인형으로 돌리고, 산 값은 낸 그대로(충전·운영자 엽전) 돌려준다.
UPDATE users SET style = 'style.doll' WHERE style = 'style.joseon';

INSERT INTO coin_ledger (user_id, delta, free_delta, balance, kind, ref, memo, created_at)
SELECT l.user_id, -l.delta, -l.free_delta, u.paid_coins + u.free_coins - l.delta, 'REFUND', l.ref, '조선 민화 화풍 판매 종료', CURRENT_TIMESTAMP
FROM coin_ledger l JOIN users u ON u.id = l.user_id
WHERE l.kind = 'SPEND' AND l.ref = 'style.joseon';

UPDATE users SET
    paid_coins = paid_coins + (SELECT COALESCE(SUM(l.free_delta - l.delta), 0) FROM coin_ledger l
        WHERE l.user_id = users.id AND l.kind = 'SPEND' AND l.ref = 'style.joseon'),
    free_coins = free_coins + (SELECT COALESCE(SUM(-l.free_delta), 0) FROM coin_ledger l
        WHERE l.user_id = users.id AND l.kind = 'SPEND' AND l.ref = 'style.joseon')
WHERE id IN (SELECT user_id FROM coin_ledger WHERE kind = 'SPEND' AND ref = 'style.joseon');

DELETE FROM user_items WHERE item_id = 'style.joseon';
