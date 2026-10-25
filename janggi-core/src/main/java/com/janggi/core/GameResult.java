package com.janggi.core;

public enum GameResult {
    ONGOING,
    CHO_WIN,
    HAN_WIN,
    /** 빅장 무승부. 레이팅에 반영하지 않는 무효 대국으로 처리한다. */
    DRAW
}
