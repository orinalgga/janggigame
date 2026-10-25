package com.janggi.server.game;

/** 온라인 대국에서 자기 차례가 아니거나 대국 참가자가 아닌 회원이 두려고 했다. */
public class NotYourGameException extends RuntimeException {

    public NotYourGameException() {
        super("이 대국에서 지금 둘 수 있는 회원이 아닙니다");
    }
}
