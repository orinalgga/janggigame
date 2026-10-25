package com.janggi.server.game;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String id) {
        super("대국을 찾을 수 없습니다: " + id);
    }
}
