package com.janggi.server.user;

/** 쓸 수 없는 닉네임. 웹에는 400과 함께 이유를 보낸다. */
public class NicknameException extends RuntimeException {

    public NicknameException(String message) {
        super(message);
    }
}
