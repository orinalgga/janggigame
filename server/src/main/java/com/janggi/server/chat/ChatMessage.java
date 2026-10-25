package com.janggi.server.chat;

/**
 * 대국 채팅 한 줄. side는 말한 쪽 진영(CHO, HAN), ai면 AI가 한 말이다.
 * id는 대국 안에서 1부터 늘어나는 번호라 웹이 같은 말을 두 번 받아도 걸러 낼 수 있다.
 */
public record ChatMessage(long id, String side, String nickname, boolean ai, String text, long at) {
}
