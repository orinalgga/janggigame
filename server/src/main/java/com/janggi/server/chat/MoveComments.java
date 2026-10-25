package com.janggi.server.chat;

import com.janggi.core.GameResult;
import com.janggi.core.Side;
import com.janggi.server.game.GameDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** AI 대국에서 수가 오갈 때 AI가 하는 한마디. 정해 둔 말이라 API를 부르지 않는다. */
final class MoveComments {

    /** 아무 일 없는 수에도 가끔 한마디 할 확률. */
    private static final double IDLE_CHANCE = 0.08;
    /** 잡으면 한마디 할 만한 알. 졸·병, 사는 넘어간다. */
    private static final List<String> NOTABLE = List.of("CHARIOT", "CANNON", "HORSE", "ELEPHANT");

    private MoveComments() {
    }

    static String line(Persona p, Side ai, GameDto before, GameDto after, Random random) {
        if (after.result() != GameResult.ONGOING) {
            return ending(p, ai, after);
        }
        List<String> aiLost = lost(before, after, ai);
        List<String> humanLost = lost(before, after, ChatService.opposite(ai));
        if (after.check() && after.sideToMove() != ai) {
            return pick(p, random,
                    List.of("장군이에요! 어떻게 막으실 거예요?", "어, 장군인가요? 장군!"),
                    List.of("장군이요. 허허.", "장군 받으시오!"),
                    List.of("장군.", "장군입니다."));
        }
        String humanNotable = notable(humanLost, ChatService.opposite(ai));
        if (humanNotable != null) {
            return pick(p, random,
                    List.of(humanNotable + "을(를) 잡았어요! 저 이거 맞게 한 거죠?"),
                    List.of(humanNotable + " 하나 잘 먹겠습니다, 허허."),
                    List.of(humanNotable + "은(는) 거두겠습니다."));
        }
        String aiNotable = notable(aiLost, ai);
        if (aiNotable != null) {
            return pick(p, random,
                    List.of("앗, 제 " + aiNotable + "!", "으앗, " + aiNotable + "을(를) 잃었어요…"),
                    List.of("어이쿠, 내 " + aiNotable + "!", "허, " + aiNotable + "을(를) 내줬네."),
                    List.of("좋은 수입니다. " + aiNotable + "을(를) 내주었군요.", "그 " + aiNotable + "은(는) 드리지요."));
        }
        return random.nextDouble() < IDLE_CHANCE ? p.idle(random) : null;
    }

    private static String ending(Persona p, Side ai, GameDto g) {
        if (g.result() == GameResult.DRAW) {
            return "비겼네요. 좋은 승부였습니다.";
        }
        boolean aiWon = (g.result() == GameResult.CHO_WIN) == (ai == Side.CHO);
        return switch (p) {
            case BEGINNER -> aiWon ? "제가 이겼어요?! 운이 좋았나 봐요. 한 판 더 해요!" : "졌어요… 정말 잘 두시네요! 다음엔 더 열심히 할게요.";
            case INTERMEDIATE -> aiWon ? "허허, 이번 판은 내가 가져가네. 한 판 더?" : "허, 졌구먼! 실력이 보통이 아니시네.";
            case ADVANCED -> aiWon ? "좋은 대국이었습니다." : "제가 졌습니다. 훌륭한 장기였습니다.";
        };
    }

    private static String pick(Persona p, Random random, List<String> beginner, List<String> intermediate,
            List<String> advanced) {
        List<String> lines = switch (p) {
            case BEGINNER -> beginner;
            case INTERMEDIATE -> intermediate;
            case ADVANCED -> advanced;
        };
        return lines.get(random.nextInt(lines.size()));
    }

    /** before에는 있었는데 after에는 없는 side의 알 종류. */
    static List<String> lost(GameDto before, GameDto after, Side side) {
        Map<String, Integer> count = new HashMap<>();
        for (GameDto.PieceDto p : before.pieces()) {
            if (p.side().equals(side.name())) {
                count.merge(p.type(), 1, Integer::sum);
            }
        }
        for (GameDto.PieceDto p : after.pieces()) {
            if (p.side().equals(side.name())) {
                count.merge(p.type(), -1, Integer::sum);
            }
        }
        List<String> lost = new ArrayList<>();
        count.forEach((type, n) -> {
            for (int i = 0; i < n; i++) {
                lost.add(type);
            }
        });
        return lost;
    }

    /** 잃은 알 가운데 가장 값진 것의 우리말 이름. 없으면 null. */
    private static String notable(List<String> lost, Side side) {
        for (String type : NOTABLE) {
            if (lost.contains(type)) {
                return korean(type, side);
            }
        }
        return null;
    }

    static String korean(String type, Side side) {
        return switch (type) {
            case "GENERAL" -> "장";
            case "GUARD" -> "사";
            case "ELEPHANT" -> "상";
            case "HORSE" -> "마";
            case "CHARIOT" -> "차";
            case "CANNON" -> "포";
            default -> side == Side.CHO ? "졸" : "병";
        };
    }
}
