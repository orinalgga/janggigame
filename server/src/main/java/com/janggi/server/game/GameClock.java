package com.janggi.server.game;

import com.janggi.core.Side;

/**
 * 기본 시간 + 초읽기 대국 시계. 기본 시간을 다 쓰면 초읽기에 들어가고, 한 수를 초읽기 한 번(예: 30초) 안에 두면
 * 횟수가 줄지 않는다. 초읽기 한 번을 넘기면 한 회가 빠지고 다음 초읽기가 시작된다. 남은 횟수를 다 쓰면 시간패.
 * 동기화는 이 시계를 가진 GameSession이 맡는다.
 */
final class GameClock {

    /**
     * at(epoch 밀리초) 시점의 시계. choMs·hanMs는 남은 기본 시간, choPeriods·hanPeriods는 남은 초읽기 횟수,
     * periodMs는 초읽기 한 번의 길이, periodLeftMs는 가고 있는 쪽이 지금 초읽기 중이면 이번 초읽기에서 남은 시간
     * (아니면 periodMs). 시간을 다 썼으면 모두 0.
     */
    record Snapshot(long choMs, long hanMs, int choPeriods, int hanPeriods, long periodMs, long periodLeftMs,
            Side running, long at) {
    }

    /** 한쪽의 지금 시간. */
    private record Left(long mainMs, int periods, long periodLeftMs) {

        boolean flagged() {
            return mainMs <= 0 && periods <= 0;
        }
    }

    private final long periodMs;
    private long choMs;
    private long hanMs;
    private int choPeriods;
    private int hanPeriods;
    private Side running;
    private long since;

    GameClock(long choMs, long hanMs, int choPeriods, int hanPeriods, long periodMs) {
        this.choMs = choMs;
        this.hanMs = hanMs;
        this.choPeriods = periodMs > 0 ? choPeriods : 0;
        this.hanPeriods = periodMs > 0 ? hanPeriods : 0;
        this.periodMs = periodMs;
    }

    /** 두 쪽 모두 같은 시간으로 시작하는 새 시계. */
    static GameClock fresh(long baseMs, int periods, long periodMs) {
        return new GameClock(baseMs, baseMs, periods, periods, periodMs);
    }

    /** side의 시계를 now부터 돌린다. */
    void start(Side side, long now) {
        running = side;
        since = now;
    }

    void stop(long now) {
        if (running != null) {
            set(running, left(running, now));
            running = null;
        }
    }

    /** 남은 기본 시간. */
    long remaining(Side side, long now) {
        return left(side, now).mainMs();
    }

    int periods(Side side, long now) {
        return left(side, now).periods();
    }

    boolean flagged(long now) {
        return running != null && left(running, now).flagged();
    }

    /** 지금 둘 차례인 쪽이 수를 뒀다. 쓴 시간을 빼고(초읽기 중이면 다음 수는 새 초읽기로) 상대 시계를 돌린다. */
    void punch(long now) {
        Side mover = running;
        set(mover, left(mover, now));
        start(mover.opponent(), now);
    }

    Snapshot snapshot(long now) {
        Left cho = left(Side.CHO, now);
        Left han = left(Side.HAN, now);
        long periodLeft = running == null ? periodMs : (running == Side.CHO ? cho : han).periodLeftMs();
        return new Snapshot(cho.mainMs(), han.mainMs(), cho.periods(), han.periods(), periodMs, periodLeft, running,
                now);
    }

    private Left left(Side side, long now) {
        long main = side == Side.CHO ? choMs : hanMs;
        int periods = side == Side.CHO ? choPeriods : hanPeriods;
        if (side != running) {
            return new Left(main, periods, periodMs);
        }
        long elapsed = now - since;
        if (elapsed < main) {
            return new Left(main - elapsed, periods, periodMs);
        }
        // 기본 시간을 넘긴 만큼을 초읽기에서 쓴다. 이번 수 안에서 초읽기 한 번을 다 쓸 때마다 한 회가 빠진다.
        long over = elapsed - Math.max(0, main);
        if (periodMs <= 0 || periods <= 0) {
            return new Left(0, 0, 0);
        }
        long used = over / periodMs;
        if (used >= periods) {
            return new Left(0, 0, 0);
        }
        return new Left(0, (int) (periods - used), periodMs - over % periodMs);
    }

    private void set(Side side, Left left) {
        if (side == Side.CHO) {
            choMs = left.mainMs();
            choPeriods = left.periods();
        } else {
            hanMs = left.mainMs();
            hanPeriods = left.periods();
        }
    }
}
