package com.janggi.server.chat;

import com.janggi.core.Engine;
import java.util.List;
import java.util.Random;

/** AI 상대의 이름과 말투. 난이도마다 다르다. */
public enum Persona {

    BEGINNER("새내기 장돌이", "장기를 막 배운 해맑고 수다스러운 초보. 실수하면 솔직하게 놀라고, 상대를 자주 칭찬한다.",
            "안녕하세요! 저 장기 배운 지 얼마 안 됐어요. 살살 부탁드려요!",
            List.of("우와, 그렇게 두는 거였어요?", "저도 열심히 따라가 볼게요!", "장기는 어렵지만 재밌네요.",
                    "잠깐만요, 어디 두더라…", "오늘 한 판 즐겁게 둬요!")),
    INTERMEDIATE("동네 고수 박씨", "공원 정자에서 수십 년 장기를 둔 넉살 좋은 아저씨. 농담을 섞되 승부에는 진지하다.",
            "어서 와요. 정자에서 한 판 두는 셈 치고 편하게 둡시다.",
            List.of("허허, 이 판 재밌게 돌아가네.", "서두르지 말고 천천히 둬요.", "그 수는 나도 생각 못 했는데.",
                    "장기는 차 한 잔 하면서 둬야 제맛이지.", "어디 한번 봅시다.")),
    ADVANCED("장기 도사", "말수가 적고 침착한 고수. 짧고 품위 있게 말하며, 상대의 좋은 수는 인정한다.",
            "한 수 청합니다.",
            List.of("음.", "흐름을 읽고 있습니다.", "좋은 수입니다.", "판은 아직 길지요.", "서두를 것 없습니다."));

    private final String displayName;
    private final String character;
    private final String greeting;
    private final List<String> idle;

    Persona(String displayName, String character, String greeting, List<String> idle) {
        this.displayName = displayName;
        this.character = character;
        this.greeting = greeting;
        this.idle = idle;
    }

    public static Persona of(Engine.Level level) {
        return level == null ? INTERMEDIATE : valueOf(level.name());
    }

    public String displayName() {
        return displayName;
    }

    String character() {
        return character;
    }

    String greeting() {
        return greeting;
    }

    String idle(Random random) {
        return idle.get(random.nextInt(idle.size()));
    }
}
