package com.janggi.server.shop;

import java.util.Arrays;
import java.util.Optional;

/**
 * 상점에서 파는 테마와 아바타. 모양은 웹의 themes.ts, avatars.ts가 같은 id로 정한다.
 * 값은 엽전(1닢 = 100원)이고, 0닢인 상품은 모두가 처음부터 가지고 있다.
 * 보상(reward) 상품은 팔지 않고 운영 대회 우승으로만(또는 운영자 지급으로) 얻는다.
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
    PIECES_GOLD("pieces.gold", Slot.PIECES, "금박 알", "금박을 입힌 화려한 알", 49),
    AVATAR_BASIC("avatar.basic", Slot.AVATAR, "기본 아바타", "닉네임 첫 글자", 0),
    AVATAR_PINE("avatar.pine", Slot.AVATAR, "소나무", "사철 푸른 소나무", 19),
    AVATAR_PLUM("avatar.plum", Slot.AVATAR, "매화", "눈 속에 피는 매화", 19),
    AVATAR_TURTLE("avatar.turtle", Slot.AVATAR, "거북", "느려도 끝내 이기는 거북", 19),
    AVATAR_MOON("avatar.moon", Slot.AVATAR, "보름달", "밤하늘에 뜬 보름달", 29),
    AVATAR_CRANE("avatar.crane", Slot.AVATAR, "학", "고고한 두루미", 29),
    AVATAR_TIGER("avatar.tiger", Slot.AVATAR, "호랑이", "산을 호령하는 호랑이", 39),
    AVATAR_DRAGON("avatar.dragon", Slot.AVATAR, "용", "구름을 부리는 청룡", 49),
    AVATAR_PHOENIX("avatar.phoenix", Slot.AVATAR, "봉황", "상서로운 봉황", 49),
    AVATAR_CROWN("avatar.crown", Slot.AVATAR, "우승 금관", "운영 대회 우승자만 쓰는 금관", 0, true),
    AVATAR_KING("avatar.king", Slot.AVATAR, "정기 리그 왕", "정기 리그 우승자만 쓰는 왕(王)", 0, true),
    AVATAR_MASTER("avatar.master", Slot.AVATAR, "국수", "특별 리그 우승자만 쓰는 국수(國手)", 0, true);

    /** 같은 칸에는 하나만 장착한다. */
    public enum Slot {
        BOARD, PIECES, AVATAR
    }

    private final String id;
    private final Slot slot;
    private final String title;
    private final String description;
    private final int price;
    private final boolean reward;

    ShopItem(String id, Slot slot, String title, String description, int price) {
        this(id, slot, title, description, price, false);
    }

    ShopItem(String id, Slot slot, String title, String description, int price, boolean reward) {
        this.reward = reward;
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
        return price == 0 && !reward;
    }

    /** 팔지 않고 대회 우승으로만 얻는 상품. */
    public boolean reward() {
        return reward;
    }
}
