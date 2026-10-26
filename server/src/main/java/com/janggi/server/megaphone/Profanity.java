package com.janggi.server.megaphone;

import java.util.List;
import java.util.Locale;

/** 확성기에 올릴 수 없는 말. 띄어쓰기·기호를 빼고 비교해서 "시 발" 같은 우회도 막는다. */
final class Profanity {

    private static final List<String> WORDS = List.of(
            "씨발", "시발", "씨빨", "ㅅㅂ", "ㅆㅂ", "병신", "븅신", "ㅂㅅ", "개새끼", "개새기", "새끼", "좆", "존나", "졸라",
            "지랄", "ㅈㄹ", "미친놈", "미친년", "닥쳐", "애미", "애비", "엠창", "느금", "fuck", "shit", "bitch");

    private static final List<String> LINKS = List.of("http", "www", ".com", ".kr", ".net", "카톡", "오픈채팅", "텔레");

    private Profanity() {
    }

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}.]", "");
    }

    static boolean rude(String text) {
        String t = normalize(text);
        return WORDS.stream().anyMatch(t::contains);
    }

    /** 광고 막기: 주소나 메신저로 끌어내는 말. */
    static boolean link(String text) {
        String t = normalize(text);
        return LINKS.stream().anyMatch(t::contains);
    }
}
