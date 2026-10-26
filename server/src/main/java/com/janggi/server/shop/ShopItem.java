package com.janggi.server.shop;

import java.util.Arrays;
import java.util.Optional;

/**
 * 상점에서 파는 테마와 캐릭터 부품. 모양은 웹의 themes.ts, character.ts가 같은 id로 정한다.
 * 값은 엽전(1닢 = 100원)이고, 캐릭터 부품은 10~50닢(1,000~5,000원)이다. 0닢인 상품은 모두가 처음부터 가지고 있다.
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
    STYLE_DOLL("style.doll", Slot.STYLE, "인형", "큰 머리의 말랑한 입체 인형", 0),
    STYLE_ANIME("style.anime", Slot.STYLE, "애니메이션", "반짝이는 큰 눈의 만화 그림체", 25),
    AVATAR_BASIC("avatar.basic", Slot.AVATAR, "기본 방", "창이 난 아늑한 내 방", 0),
    AVATAR_CAFE("avatar.cafe", Slot.AVATAR, "카페", "벽돌 벽에 조명이 걸린 카페", 15),
    AVATAR_BEACH("avatar.beach", Slot.AVATAR, "바닷가", "야자수가 서 있는 여름 바다", 20),
    AVATAR_GOLF("avatar.golf", Slot.AVATAR, "골프장", "깃발이 꽂힌 푸른 그린", 25),
    AVATAR_CITY("avatar.city", Slot.AVATAR, "도시 야경", "불 켜진 빌딩 숲의 밤", 25),
    AVATAR_PLUM("avatar.plum", Slot.AVATAR, "매화도", "고목에 핀 홍매화", 15),
    AVATAR_MOON("avatar.moon", Slot.AVATAR, "월하산수", "보름달 뜬 밤 산", 15),
    AVATAR_TURTLE("avatar.turtle", Slot.AVATAR, "수묵 산수화", "안개 낀 먹빛 산수", 20),
    AVATAR_PINE("avatar.pine", Slot.AVATAR, "송학도", "노송 위로 나는 학", 20),
    AVATAR_CRANE("avatar.crane", Slot.AVATAR, "십장생도", "해와 산, 학이 어우러진 장수 그림", 25),
    AVATAR_TIGER("avatar.tiger", Slot.AVATAR, "까치호랑이", "민화 속 호랑이와 까치", 30),
    AVATAR_DRAGON("avatar.dragon", Slot.AVATAR, "운룡도", "구름 속을 나는 청룡", 35),
    AVATAR_PHOENIX("avatar.phoenix", Slot.AVATAR, "봉황도", "붉은 해 아래 봉황", 35),
    AVATAR_IRWOROBONG("avatar.irworobong", Slot.AVATAR, "일월오봉도", "임금의 자리 뒤에 두던 해·달·다섯 봉우리 병풍", 50),
    AVATAR_CROWN("avatar.crown", Slot.AVATAR, "우승 금관", "운영 대회 우승자만 두는 금박 병풍", 0, true),
    AVATAR_KING("avatar.king", Slot.AVATAR, "정기 리그 왕", "정기 리그 우승자만 두는 자줏빛 비단", 0, true),
    AVATAR_MASTER("avatar.master", Slot.AVATAR, "국수", "특별 리그 우승자만 두는 묵향 배경", 0, true),
    BODY_MAN("body.man", Slot.BODY, "남자", "까만 머리의 남자", 0),
    BODY_WOMAN("body.woman", Slot.BODY, "여자", "까만 머리의 여자", 0),
    BODY_ELDER("body.elder", Slot.BODY, "은발 남자", "희끗한 머리의 남자", 0),
    BODY_ELDER_WOMAN("body.elder.woman", Slot.BODY, "은발 여자", "희끗한 머리의 여자", 0),
    BODY_CAT("body.cat", Slot.BODY, "고양이", "귀가 쫑긋한 치즈 고양이", 20),
    BODY_DOG("body.dog", Slot.BODY, "강아지", "귀가 축 처진 순한 강아지", 20),
    BODY_RABBIT("body.rabbit", Slot.BODY, "토끼", "긴 귀의 하얀 토끼", 20),
    BODY_BEAR("body.bear", Slot.BODY, "곰", "듬직한 갈색 곰", 20),
    BODY_FOX("body.fox", Slot.BODY, "여우", "꼬리가 탐스러운 붉은 여우", 25),
    BODY_PANDA("body.panda", Slot.BODY, "판다", "눈가가 까만 판다", 25),
    BODY_TIGER("body.tiger", Slot.BODY, "호랑이", "줄무늬가 멋진 호랑이", 30),
    HAIR_SHORT("hair.short", Slot.HAIR, "짧은 머리", "단정하게 자른 머리", 0),
    HAIR_BOB("hair.bob", Slot.HAIR, "단발머리", "앞머리를 내린 단발", 0),
    HAIR_PERM("hair.perm", Slot.HAIR, "뽀글 파마", "풍성하게 볶은 파마", 15),
    HAIR_LONG("hair.long", Slot.HAIR, "긴 생머리", "허리까지 내려오는 머리", 20),
    HAIR_SIDEPART("hair.sidepart", Slot.HAIR, "가르마", "옆으로 넘긴 단정한 가르마", 10),
    HAIR_SLICK("hair.slick", Slot.HAIR, "올백", "윤기 나게 넘긴 올백", 15),
    HAIR_BUZZ("hair.buzz", Slot.HAIR, "스포츠머리", "짧게 깎은 시원한 머리", 10),
    HAIR_PONYTAIL("hair.ponytail", Slot.HAIR, "포니테일", "높이 묶은 꽁지머리", 15),
    HAIR_WAVE("hair.wave", Slot.HAIR, "물결 웨이브", "굵게 물결치는 긴 머리", 25),
    HAIR_BUN("hair.bun", Slot.HAIR, "똥머리", "정수리에 둥글게 올린 머리", 15),
    HAIR_SANGTU("hair.sangtu", Slot.HAIR, "상투와 망건", "금관자를 단 망건", 0),
    HAIR_JJOK("hair.jjok", Slot.HAIR, "쪽진머리", "옥비녀를 꽂은 쪽", 0),
    HAIR_LOOSE("hair.loose", Slot.HAIR, "무관 머리", "붉은 머리띠로 묶은 머리", 15),
    HAIR_GACHE("hair.gache", Slot.HAIR, "가채", "궁중 여인의 큰머리", 30),
    BEARD_NONE("beard.none", Slot.BEARD, "수염 없음", "말끔한 얼굴", 0),
    BEARD_MUSTACHE("beard.mustache", Slot.BEARD, "콧수염", "단정한 팔자 콧수염", 10),
    BEARD_SCHOLAR("beard.scholar", Slot.BEARD, "선비 수염", "가늘고 긴 염소수염", 15),
    BEARD_FULL("beard.full", Slot.BEARD, "장군 수염", "위엄 있는 구레나룻", 20),
    BEARD_SAGE("beard.sage", Slot.BEARD, "신선 수염", "가슴까지 내려오는 흰 수염", 25),
    OUTFIT_TEE("outfit.tee", Slot.OUTFIT, "흰 티와 청바지", "어디에나 어울리는 기본 차림", 0),
    OUTFIT_HOODIE("outfit.hoodie", Slot.OUTFIT, "후드티", "편한 후드티와 조거 바지", 15),
    OUTFIT_KNIT("outfit.knit", Slot.OUTFIT, "니트와 슬랙스", "셔츠 깃을 낸 베이지 니트", 20),
    OUTFIT_SUIT("outfit.suit", Slot.OUTFIT, "정장", "붉은 넥타이를 맨 남색 정장", 25),
    OUTFIT_DRESS("outfit.dress", Slot.OUTFIT, "원피스", "꽃무늬 분홍 원피스", 25),
    OUTFIT_GOLF("outfit.golf", Slot.OUTFIT, "골프웨어", "줄무늬 폴로셔츠와 면바지", 30),
    OUTFIT_PLAIN("outfit.plain", Slot.OUTFIT, "저고리", "남자는 무명 바지저고리, 여자는 노랑 저고리에 다홍치마", 0),
    OUTFIT_DURUMAGI("outfit.durumagi", Slot.OUTFIT, "자주 두루마기", "점잖은 자줏빛 겉옷", 15),
    OUTFIT_DOPO("outfit.dopo", Slot.OUTFIT, "옥색 도포", "쪽빛 세조대를 맨 선비 도포", 20),
    OUTFIT_CHEOLLIK("outfit.cheollik", Slot.OUTFIT, "무관 철릭", "붉은 띠를 두른 무관 옷", 25),
    OUTFIT_DANGUI("outfit.dangui", Slot.OUTFIT, "금박 녹당의", "금박 무늬 초록 당의와 홍치마", 30),
    OUTFIT_GWANBOK("outfit.gwanbok", Slot.OUTFIT, "홍단령 관복", "쌍학 흉배를 단 당상관 관복", 35),
    OUTFIT_ARMOR("outfit.armor", Slot.OUTFIT, "두정갑", "금빛 징을 박은 장수의 갑옷", 40),
    OUTFIT_WONSAM("outfit.wonsam", Slot.OUTFIT, "원삼", "색동 소매에 금박을 올린 예복", 45),
    OUTFIT_GONRYONGPO("outfit.gonryongpo", Slot.OUTFIT, "곤룡포", "가슴과 어깨에 용보를 단 임금의 옷", 50),
    HAT_NONE("hat.none", Slot.HAT, "없음", "맨머리", 0),
    HAT_CAP("hat.cap", Slot.HAT, "야구 모자", "파란 야구 모자", 10),
    HAT_BEANIE("hat.beanie", Slot.HAT, "방울 비니", "방울 달린 빨간 털모자", 10),
    HAT_GLASSES("hat.glasses", Slot.HAT, "뿔테 안경", "점잖은 검은 뿔테", 10),
    HAT_SUNGLASSES("hat.sunglasses", Slot.HAT, "선글라스", "멋을 낸 까만 선글라스", 15),
    HAT_GAT("hat.gat", Slot.HAT, "흑립", "호박 갓끈을 단 선비의 갓", 15),
    HAT_BINYEO("hat.binyeo", Slot.HAT, "금봉잠", "봉황을 새긴 금비녀", 20),
    HAT_JEONGJAGWAN("hat.jeongjagwan", Slot.HAT, "정자관", "학자가 집에서 쓰던 관", 20),
    HAT_JOKDURI("hat.jokduri", Slot.HAT, "족두리", "칠보를 단 족두리", 25),
    HAT_SAMO("hat.samo", Slot.HAT, "사모", "관리가 쓰던 날개 달린 관모", 30),
    HAT_JEONRIP("hat.jeonrip", Slot.HAT, "전립", "공작 깃과 상모를 단 무관의 모자", 30),
    HAT_TTEOLJAM("hat.tteoljam", Slot.HAT, "떨잠", "칠보 구슬이 떨리는 머리 장식", 35),
    HAT_IKSEON("hat.ikseon", Slot.HAT, "익선관", "임금이 쓰던 관", 50);

    /** 같은 칸에는 하나만 장착한다. */
    public enum Slot {
        BOARD, PIECES, STYLE, AVATAR, BODY, HAIR, BEARD, OUTFIT, HAT
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
        return this == BOARD_CLASSIC || this == PIECES_CLASSIC || this == STYLE_DOLL || this == AVATAR_BASIC || this == BODY_MAN
                || this == HAIR_SHORT || this == BEARD_NONE || this == OUTFIT_TEE || this == HAT_NONE;
    }

    /** 팔지 않고 대회 우승으로만 얻는 상품. */
    public boolean reward() {
        return reward;
    }
}
