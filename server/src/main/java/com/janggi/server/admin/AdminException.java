package com.janggi.server.admin;

public class AdminException extends RuntimeException {

    AdminException(String message) {
        super(message);
    }

    /** 관리자가 아닌 회원이 관리자 기능을 부를 때. */
    public static class Forbidden extends AdminException {
        Forbidden() {
            super("관리자만 쓸 수 있어요");
        }
    }

    public static class BadRequest extends AdminException {
        BadRequest(String message) {
            super(message);
        }
    }
}
