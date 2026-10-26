package com.janggi.server.invite;

/** 초대를 보내거나 받을 수 없을 때. 메시지는 그대로 화면에 보여 준다. */
public class InviteException extends RuntimeException {

    public InviteException(String message) {
        super(message);
    }
}
