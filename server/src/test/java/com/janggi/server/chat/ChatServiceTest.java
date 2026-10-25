package com.janggi.server.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static ChatMessage m(long id, boolean ai, String text) {
        return new ChatMessage(id, ai ? "HAN" : "CHO", "", ai, text, 0);
    }

    @Test
    void turnsStartWithUserAndMergeSameSide() {
        List<ClaudeChat.Turn> turns = ChatService.turns(List.of(
                m(1, true, "AI의 한마디"),
                m(2, false, "안녕"),
                m(3, false, "잘 부탁해요"),
                m(4, true, "반갑습니다"),
                m(5, true, "좋은 대국 해요"),
                m(6, false, "네")));
        assertEquals(3, turns.size());
        assertFalse(turns.get(0).ai());
        assertEquals("안녕\n잘 부탁해요", turns.get(0).text());
        assertEquals("반갑습니다\n좋은 대국 해요", turns.get(1).text());
        assertEquals("네", turns.get(2).text());
    }
}
