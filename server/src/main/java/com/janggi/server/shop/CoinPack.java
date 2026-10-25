package com.janggi.server.shop;

import java.util.Arrays;
import java.util.Optional;

/** 엽전 충전 상품. 100원에 1닢이다. */
public enum CoinPack {
    COINS_10(10), COINS_30(30), COINS_50(50), COINS_100(100), COINS_300(300);

    /** 엽전 1닢의 값(원). */
    public static final int WON_PER_COIN = 100;

    private final int coins;

    CoinPack(int coins) {
        this.coins = coins;
    }

    public static Optional<CoinPack> byId(String id) {
        return Arrays.stream(values()).filter(p -> p.id().equals(id)).findFirst();
    }

    public String id() {
        return "coins." + coins;
    }

    public String title() {
        return "엽전 " + coins + "닢";
    }

    public int coins() {
        return coins;
    }

    /** 원 단위 결제 금액. */
    public int price() {
        return coins * WON_PER_COIN;
    }
}
