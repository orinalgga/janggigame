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
    AVATAR_BASIC("avatar.basic", Slot.AVATAR, "기본 배경", "따뜻한 한지색", 0),
    AVATAR_PINE("avatar.pine", Slot.AVATAR, "소나무 문양", "사철 푸른 소나무", 19),
    AVATAR_PLUM("avatar.plum", Slot.AVATAR, "매화 문양", "눈 속에 피는 매화", 19),
    AVATAR_TURTLE("avatar.turtle", Slot.AVATAR, "거북 문양", "느려도 끝내 이기는 거북", 19),
    AVATAR_MOON("avatar.moon", Slot.AVATAR, "보름달 문양", "밤하늘에 뜬 보름달", 29),
    AVATAR_CRANE("avatar.crane", Slot.AVATAR, "학 문양", "고고한 두루미", 29),
    AVATAR_TIGER("avatar.tiger", Slot.AVATAR, "호랑이 문양", "산을 호령하는 호랑이", 39),
    AVATAR_DRAGON("avatar.dragon", Slot.AVATAR, "용 문양", "구름을 부리는 청룡", 49),
    AVATAR_PHOENIX("avatar.phoenix", Slot.AVATAR, "봉황 문양", "상서로운 봉황", 49),
    AVATAR_CROWN("avatar.crown", Slot.AVATAR, "우승 금관", "운영 대회 우승자만 쓰는 금관", 0, true),
    AVATAR_KING("avatar.king", Slot.AVATAR, "정기 리그 왕", "정기 리그 우승자만 쓰는 왕(王)", 0, true),
    AVATAR_MASTER("avatar.master", Slot.AVATAR, "국수", "특별 리그 우승자만 쓰는 국수(國手)", 0, true),
    BODY_HUMAN("body.human", Slot.BODY, "사람", "뽀얀 얼굴", 0),
    BODY_HUMAN_TAN("body.human.tan", Slot.BODY, "사람(구릿빛)", "햇볕에 그을린 얼굴", 0),
    BODY_RABBIT("body.rabbit", Slot.BODY, "토끼", "귀가 긴 하얀 토끼", 29),
    BODY_CAT("body.cat", Slot.BODY, "고양이", "줄무늬 회색 고양이", 29),
    BODY_BEAR("body.bear", Slot.BODY, "곰", "듬직한 반달곰", 29),
    BODY_FOX("body.fox", Slot.BODY, "여우", "꾀 많은 붉은 여우", 39),
    BODY_TIGER("body.tiger", Slot.BODY, "호랑이", "이마에 王 자가 있는 호랑이", 39),
    BODY_DRAGON("body.dragon", Slot.BODY, "용", "금빛 뿔이 난 아기 청룡", 59),
    HAIR_BOB("hair.bob", Slot.HAIR, "단발", "가지런한 앞머리 단발", 0),
    HAIR_SANGTU("hair.sangtu", Slot.HAIR, "상투", "단정하게 틀어 올린 상투", 0),
    HAIR_DAENGGI("hair.daenggi", Slot.HAIR, "댕기머리", "붉은 댕기를 맨 땋은 머리", 19),
    HAIR_MESSY("hair.messy", Slot.HAIR, "더벅머리", "삐죽삐죽 갈색 더벅머리", 19),
    HAIR_TWIN("hair.twin", Slot.HAIR, "양갈래", "리본 묶은 양갈래", 29),
    HAIR_JJOK("hair.jjok", Slot.HAIR, "쪽진머리", "옥비녀를 꽂은 쪽진머리", 29),
    HAIR_LONG("hair.long", Slot.HAIR, "긴 생머리", "허리까지 내려오는 검은 머리", 29),
    HAIR_SILVER("hair.silver", Slot.HAIR, "은발", "신선 같은 긴 은발", 39),
    OUTFIT_WHITE("outfit.white", Slot.OUTFIT, "흰 저고리", "수수한 무명 저고리", 0),
    OUTFIT_SAEKDONG("outfit.saekdong", Slot.OUTFIT, "색동 저고리", "알록달록 색동 소매에 다홍치마", 29),
    OUTFIT_PINK("outfit.pink", Slot.OUTFIT, "분홍 치마저고리", "연분홍 저고리와 꽃분홍 치마", 29),
    OUTFIT_DOPO("outfit.dopo", Slot.OUTFIT, "선비 도포", "옥색 도포에 쪽빛 세조대", 29),
    OUTFIT_BLACK("outfit.black", Slot.OUTFIT, "무사 흑의", "붉은 띠를 맨 검은 무사복", 39),
    OUTFIT_HWAROT("outfit.hwarot", Slot.OUTFIT, "활옷", "금박 꽃무늬의 화려한 혼례복", 49),
    OUTFIT_ARMOR("outfit.armor", Slot.OUTFIT, "장군 갑옷", "금빛 비늘에 호심경을 단 갑옷", 59),
    OUTFIT_GONRYONGPO("outfit.gonryongpo", Slot.OUTFIT, "곤룡포", "금빛 용보를 단 임금의 옷", 69),
    HAT_NONE("hat.none", Slot.HAT, "모자 없음", "맨머리", 0),
    HAT_FLOWER("hat.flower", Slot.HAT, "꽃핀", "분홍 꽃 머리핀", 19),
    HAT_GAT("hat.gat", Slot.HAT, "갓", "선비의 검은 갓", 29),
    HAT_JOKDURI("hat.jokduri", Slot.HAT, "족두리", "구슬 단 족두리", 29),
    HAT_HWAGWAN("hat.hwagwan", Slot.HAT, "화관", "꽃을 엮은 화관", 39),
    HAT_HELMET("hat.helmet", Slot.HAT, "투구", "붉은 상모를 단 장군 투구", 49),
    HAT_IKSEON("hat.ikseon", Slot.HAT, "익선관", "임금이 쓰던 익선관", 59);

    /** 같은 칸에는 하나만 장착한다. */
    public enum Slot {
        BOARD, PIECES, AVATAR, BODY, HAIR, OUTFIT, HAT
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

    /** 로그인하지 않은 사람이 기본으로 입고 있는 것. 칸마다 하나. */
    public boolean starter() {
        return this == BOARD_CLASSIC || this == PIECES_CLASSIC || this == AVATAR_BASIC || this == BODY_HUMAN
                || this == HAIR_BOB || this == OUTFIT_WHITE || this == HAT_NONE;
    }

    /** 팔지 않고 대회 우승으로만 얻는 상품. */
    public boolean reward() {
        return reward;
    }
}
