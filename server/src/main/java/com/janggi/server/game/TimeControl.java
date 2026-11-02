package com.janggi.server.game;

import java.util.List;

/** 회원이 고를 수 있는 대국 기본 시간. 초읽기(30초 3회)는 모든 온라인 대국에서 같다. */
public final class TimeControl {

    /** 1분, 5분, 10분. */
    public static final List<Integer> BASE_CHOICES = List.of(60, 300, 600);

    public static final int DEFAULT_BASE = 600;

    private TimeControl() {
    }

    /** 고를 수 있는 시간이 아니면 기본 10분. */
    public static int baseOrDefault(Integer seconds) {
        return seconds != null && BASE_CHOICES.contains(seconds) ? seconds : DEFAULT_BASE;
    }
}
