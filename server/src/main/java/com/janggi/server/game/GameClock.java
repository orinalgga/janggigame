package com.janggi.server.game;

import com.janggi.core.Side;

/**
 * 피셔 방식 대국 시계. 수를 두면 쓴 시간만큼 빠지고 증초가 더해진다.
 * 동기화는 이 시계를 가진 GameSession이 맡는다.
 */
final class GameClock {

    /** at(epoch 밀리초) 시점의 남은 시간과 가고 있는 쪽. 다 썼으면 0. */
    record Snapshot(long choMs, long hanMs, Side running, long at) {
    }

    private final long incrementMs;
    private long choMs;
    private long hanMs;
    private Side running;
    private long since;

    GameClock(long choMs, long hanMs, long incrementMs) {
        this.choMs = choMs;
        this.hanMs = hanMs;
        this.incrementMs = incrementMs;
    }

    /** side의 시계를 now부터 돌린다. */
    void start(Side side, long now) {
        running = side;
        since = now;
    }

    void stop(long now) {
        if (running != null) {
            set(running, remaining(running, now));
            running = null;
        }
    }

    long remaining(Side side, long now) {
        long left = side == Side.CHO ? choMs : hanMs;
        return side == running ? left - (now - since) : left;
    }

    boolean flagged(long now) {
        return running != null && remaining(running, now) <= 0;
    }

    /** 지금 둘 차례인 쪽이 수를 뒀다. 시간을 빼고 증초를 더한 뒤 상대 시계를 돌린다. */
    void punch(long now) {
        Side mover = running;
        set(mover, remaining(mover, now) + incrementMs);
        start(mover.opponent(), now);
    }

    Snapshot snapshot(long now) {
        return new Snapshot(Math.max(0, remaining(Side.CHO, now)), Math.max(0, remaining(Side.HAN, now)), running, now);
    }

    private void set(Side side, long ms) {
        if (side == Side.CHO) {
            choMs = ms;
        } else {
            hanMs = ms;
        }
    }
}
