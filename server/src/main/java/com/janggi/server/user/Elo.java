package com.janggi.server.user;

/** 일로(Elo) 레이팅. 새로 시작한 회원도 빨리 제자리를 찾도록 K는 32로 둔다. */
public final class Elo {

    static final int K = 32;

    private Elo() {
    }

    /** score는 이기면 1, 비기면 0.5, 지면 0. 내 레이팅이 바뀌는 양을 돌려준다. */
    public static int change(int mine, int theirs, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10, (theirs - mine) / 400.0));
        return (int) Math.round(K * (score - expected));
    }
}
