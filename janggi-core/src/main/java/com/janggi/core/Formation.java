package com.janggi.core;

import static com.janggi.core.PieceType.ELEPHANT;
import static com.janggi.core.PieceType.HORSE;

/** 상차림. 각 진영이 자기 쪽에서 바라본 왼쪽부터 마·상 배치를 정한다. */
public enum Formation {
    SANG_MA_SANG_MA(ELEPHANT, HORSE, ELEPHANT, HORSE),
    MA_SANG_MA_SANG(HORSE, ELEPHANT, HORSE, ELEPHANT),
    MA_SANG_SANG_MA(HORSE, ELEPHANT, ELEPHANT, HORSE),
    SANG_MA_MA_SANG(ELEPHANT, HORSE, HORSE, ELEPHANT);

    private final PieceType[] fromLeft;

    Formation(PieceType... fromLeft) {
        this.fromLeft = fromLeft;
    }

    /** 자기 쪽에서 본 왼쪽부터 i번째(0~3) 자리의 기물. */
    PieceType at(int i) {
        return fromLeft[i];
    }
}
