package com.janggi.server.shop;

import java.util.Arrays;
import java.util.Optional;

/**
 * 상점에서 파는 테마. 모양(색)은 웹의 themes.ts가 같은 id로 정한다.
 * 값은 엽전(1닢 = 100원)이고, 0닢인 상품은 모두가 처음부터 가지고 있다.
 */
public enum ShopItem {
    BOARD_CLASSIC("board.classic", Slot.BOARD, "기본 나무판", "결이 고운 밝은 나무판", 0),
    BOARD_WALNUT("board.walnut", Slot.BOARD, "호두나무판", "짙고 묵직한 호두나무 결", 29),
    BOARD_HANJI("board.hanji", Slot.BOARD, "한지판", "먹선이 또렷한 하얀 한지", 29),
    BOARD_JADE("board.jade", Slot.BOARD, "옥돌판", "서늘한 초록빛 옥돌", 39),
    BOARD_NIGHT("board.night", Slot.BOARD, "밤하늘판", "금빛 선이 흐르는 남색 판", 39),
    PIECES_CLASSIC("pieces.classic", Slot.PIECES, "기본 팔각 알", "초록 초, 빨강 한의 기본 알", 0),
    PIECES_IVORY("pieces.ivory", Slot.PIECES, "상아 알", "따뜻한 상아색에 짙은 글씨", 29),
    PIECES_CELADON("pieces.celadon", Slot.PIECES, "청자 알", "비취빛 청자 유약", 39),
    PIECES_INK("pieces.ink", Slot.PIECES, "먹빛 알", "검은 알에 밝게 빛나는 글씨", 39),
    PIECES_GOLD("pieces.gold", Slot.PIECES, "금박 알", "금박을 입힌 화려한 알", 49);

    /** 같은 칸에는 하나만 장착한다. */
    public enum Slot {
        BOARD, PIECES
    }

    private final String id;
    private final Slot slot;
    private final String title;
    private final String description;
    private final int price;

    ShopItem(String id, Slot slot, String title, String description, int price) {
        this.id = id;
        this.slot = slot;
        this.title = title;
        this.description = description;
        this.price = price;
    }

    public static Optional<ShopItem> byId(String id) {
        return Arrays.stream(values()).filter(i -> i.id.equals(id)).findFirst();
    }

    public String id() {
        return id;
    }

    public Slot slot() {
        return slot;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** 엽전 가격. */
    public int price() {
        return price;
    }

    public boolean free() {
        return price == 0;
    }
}
