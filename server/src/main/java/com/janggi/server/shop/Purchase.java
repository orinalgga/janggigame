package com.janggi.server.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 주문 하나. 금액은 주문할 때의 가격으로 고정해 두고, 결제사 승인 금액과 맞춰 본다. */
@Entity
@Table(name = "purchases")
public class Purchase {

    public enum Status {
        PENDING, PAID, FAILED
    }

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    protected Purchase() {
    }

    public Purchase(String orderId, long userId, ShopItem item, String provider, OffsetDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.itemId = item.id();
        this.amount = item.price();
        this.provider = provider;
        this.status = Status.PENDING;
        this.createdAt = createdAt;
    }

    public void paid(String paymentKey, OffsetDateTime at) {
        this.status = Status.PAID;
        this.paymentKey = paymentKey;
        this.paidAt = at;
    }

    public void failed() {
        this.status = Status.FAILED;
    }

    public String getOrderId() {
        return orderId;
    }

    public long getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public String getProvider() {
        return provider;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public Status getStatus() {
        return status;
    }
}
