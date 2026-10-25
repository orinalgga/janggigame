package com.janggi.core;

/** 초(楚)가 먼저 둔다. 초는 판 아래쪽(y=0~3), 한은 위쪽(y=6~9)에서 시작한다. */
public enum Side {
    CHO, HAN;

    public Side opponent() {
        return this == CHO ? HAN : CHO;
    }

    /** 졸·병이 앞으로 가는 y 방향. */
    public int forward() {
        return this == CHO ? 1 : -1;
    }
}
