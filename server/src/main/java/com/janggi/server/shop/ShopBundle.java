package com.janggi.server.shop;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 캐릭터 세트. 들어 있는 부품을 한꺼번에 사면 값을 깎아 준다.
 * 이미 가진 부품은 빼고, 남은 부품 값의 합에서 DISCOUNT_PERCENT만큼 깎는다.
 */
public enum ShopBundle {
    SEONBI("set.seonbi", "선비 세트", "옥색 도포에 흑립, 선비 수염과 송학도",
            ShopItem.OUTFIT_DOPO, ShopItem.HAT_GAT, ShopItem.BEARD_SCHOLAR, ShopItem.AVATAR_PINE),
    LADY("set.lady", "정경부인 세트", "금박 녹당의에 금봉잠, 매화도",
            ShopItem.OUTFIT_DANGUI, ShopItem.HAT_BINYEO, ShopItem.AVATAR_PLUM),
    OFFICIAL("set.official", "당상관 세트", "쌍학 흉배 관복에 사모, 콧수염과 십장생도",
            ShopItem.OUTFIT_GWANBOK, ShopItem.HAT_SAMO, ShopItem.BEARD_MUSTACHE, ShopItem.AVATAR_CRANE),
    GENERAL("set.general", "장군 세트", "두정갑에 전립, 장군 수염과 까치호랑이",
            ShopItem.OUTFIT_ARMOR, ShopItem.HAT_JEONRIP, ShopItem.BEARD_FULL, ShopItem.AVATAR_TIGER),
    QUEEN("set.queen", "왕비 세트", "원삼에 가채와 떨잠, 봉황도",
            ShopItem.OUTFIT_WONSAM, ShopItem.HAIR_GACHE, ShopItem.HAT_TTEOLJAM, ShopItem.AVATAR_PHOENIX),
    KING("set.king", "임금 세트", "곤룡포에 익선관, 일월오봉도",
            ShopItem.OUTFIT_GONRYONGPO, ShopItem.HAT_IKSEON, ShopItem.AVATAR_IRWOROBONG);

    public static final int DISCOUNT_PERCENT = 20;

    private final String id;
    private final String title;
    private final String description;
    private final List<ShopItem> items;

    ShopBundle(String id, String title, String description, ShopItem... items) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.items = List.of(items);
    }

    public static Optional<ShopBundle> byId(String id) {
        return Arrays.stream(values()).filter(b -> b.id.equals(id)).findFirst();
    }

    /** 부품 값 합에서 깎은 값. 끝자리는 버린다. */
    public static int discounted(int sum) {
        return sum * (100 - DISCOUNT_PERCENT) / 100;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public List<ShopItem> items() {
        return items;
    }
}
