package com.janggi.core;

public enum GameResult {
    ONGOING,
    CHO_WIN,
    HAN_WIN,
    /** 빅장에서 점수가 같을 때. 한의 덤 때문에 실제로는 나오지 않는다. */
    DRAW
}
