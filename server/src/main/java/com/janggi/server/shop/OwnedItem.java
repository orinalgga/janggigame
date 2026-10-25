package com.janggi.server.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 회원이 가진 상품 하나. 무료 상품은 따로 남기지 않는다. */
@Entity
@Table(name = "user_items")
public class OwnedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "acquired_at", nullable = false)
    private OffsetDateTime acquiredAt;

    protected OwnedItem() {
    }

    public OwnedItem(long userId, String itemId, OffsetDateTime acquiredAt) {
        this.userId = userId;
        this.itemId = itemId;
        this.acquiredAt = acquiredAt;
    }

    public String getItemId() {
        return itemId;
    }
}
