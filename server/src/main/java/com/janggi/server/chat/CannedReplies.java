package com.janggi.server.chat;

import java.util.Random;

/** Claude를 쓰지 않을 때 AI가 하는 답. 들어온 말에 든 낱말로 고른다. */
final class CannedReplies {

    private CannedReplies() {
    }

    static String answer(Persona p, String text, Random random) {
        String t = text.toLowerCase();
        if (has(t, "안녕", "반가", "하이", "hi", "hello")) {
            return switch (p) {
                case BEGINNER -> "안녕하세요! 저랑 장기 둬 주셔서 고마워요.";
                case INTERMEDIATE -> "어서 와요, 한 판 재밌게 둡시다.";
                case ADVANCED -> "반갑습니다. 좋은 대국 부탁드립니다.";
            };
        }
        if (has(t, "봐줘", "살살", "봐 줘", "져줘", "져 줘")) {
            return switch (p) {
                case BEGINNER -> "저도 봐줄 실력이 아니에요, 헤헤.";
                case INTERMEDIATE -> "허허, 장기판에 봐주는 법은 없지.";
                case ADVANCED -> "승부는 승부입니다.";
            };
        }
        if (has(t, "잘", "고수", "세다", "강하", "대단")) {
            return switch (p) {
                case BEGINNER -> "정말요? 칭찬 들으니 힘이 나요!";
                case INTERMEDIATE -> "허허, 그쪽도 만만치 않은데.";
                case ADVANCED -> "과찬이십니다.";
            };
        }
        if (has(t, "고마", "감사", "수고", "gg", "ㅈㅈ")) {
            return switch (p) {
                case BEGINNER -> "저도 고마워요! 또 둬요!";
                case INTERMEDIATE -> "수고했어요, 다음에 또 한 판!";
                case ADVANCED -> "수고하셨습니다.";
            };
        }
        if (has(t, "ㅋ", "ㅎ", "하하", "헤헤")) {
            return switch (p) {
                case BEGINNER -> "헤헤, 저도 재밌어요!";
                case INTERMEDIATE -> "허허허.";
                case ADVANCED -> "즐거우시다니 다행입니다.";
            };
        }
        if (t.contains("?")) {
            return switch (p) {
                case BEGINNER -> "음… 저도 잘 모르겠어요. 같이 생각해 봐요!";
                case INTERMEDIATE -> "글쎄, 판을 보면 답이 나오지 않겠소?";
                case ADVANCED -> "판 위에 답이 있습니다.";
            };
        }
        return p.idle(random);
    }

    private static boolean has(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
