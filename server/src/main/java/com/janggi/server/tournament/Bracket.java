package com.janggi.server.tournament;

import java.util.ArrayList;
import java.util.List;

/** 대진표 계산. */
final class Bracket {

    private Bracket() {
    }

    /** n명이 들어가는 대진표 크기(2의 거듭제곱). */
    static int size(int n) {
        int size = 2;
        while (size < n) {
            size *= 2;
        }
        return size;
    }

    /** 라운드 수. 16칸이면 4. */
    static int rounds(int size) {
        return Integer.numberOfTrailingZeros(size);
    }

    /**
     * 첫 라운드 자리 순서대로 놓을 시드 번호(1부터). 1번과 2번 시드는 결승에서야 만나고,
     * 사람이 모자라 비는 자리(시드 번호가 n보다 큰 곳)는 높은 시드의 부전승이 된다.
     * 8칸이면 [1, 8, 4, 5, 2, 7, 3, 6].
     */
    static List<Integer> order(int size) {
        List<Integer> seeds = new ArrayList<>(List.of(1));
        while (seeds.size() < size) {
            int next = seeds.size() * 2 + 1;
            List<Integer> grown = new ArrayList<>();
            for (int s : seeds) {
                grown.add(s);
                grown.add(next - s);
            }
            seeds = grown;
        }
        return seeds;
    }
}
