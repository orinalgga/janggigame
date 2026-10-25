package com.janggi.server.chat;

/** 보낼 수 없는 채팅(빈 말, 너무 긴 말, 채팅이 없는 대국). */
public class ChatException extends RuntimeException {

    public ChatException(String message) {
        super(message);
    }
}
