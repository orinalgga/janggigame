package com.janggi.server.user;

/**
 * 장기 급수. 레이팅에 따라 18급부터 1급, 그다음 초단부터 9단까지 저절로 오르내린다.
 * 800점 아래가 18급이고 50점마다 한 칸씩 오른다. 처음 레이팅 1200점은 9급, 1650점이 초단, 2050점부터 9단이다.
 */
public final class Grade {

    private static final int FLOOR = 800;
    private static final int STEP = 50;
    private static final int KYU = 18;
    private static final int DAN = 9;

    private Grade() {
    }

    /** 0이 18급, 17이 1급, 18이 초단, 26이 9단. */
    public static int level(int rating) {
        if (rating < FLOOR) {
            return 0;
        }
        return Math.min(1 + (rating - FLOOR) / STEP, KYU + DAN - 1);
    }

    public static String name(int rating) {
        int level = level(rating);
        if (level < KYU) {
            return (KYU - level) + "급";
        }
        int dan = level - KYU + 1;
        return dan == 1 ? "초단" : dan + "단";
    }
}
