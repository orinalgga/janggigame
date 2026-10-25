package com.janggi.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 반복 심화 + 알파베타 탐색으로 수를 고르는 장기 AI.
 * 점수 단위는 1/100점(차 = 1300)이고, 늘 수를 둘 차례인 쪽 기준이다.
 */
public final class Engine {

    /** 난이도. 깊이·시간·실수 확률로 강도를 조절한다. */
    public enum Level {
        BEGINNER(1, 300, 0.30, 250),
        INTERMEDIATE(2, 1000, 0.10, 80),
        ADVANCED(5, 2000, 0.0, 0);

        final int maxDepth;
        final long timeMillis;
        /** 이 확률로 아무 합법 수나 둔다. */
        final double blunderRate;
        /** 최선 수와 이만큼 차이 나는 수까지 후보로 보고 그중에서 고른다. */
        final int tolerance;

        Level(int maxDepth, long timeMillis, double blunderRate, int tolerance) {
            this.maxDepth = maxDepth;
            this.timeMillis = timeMillis;
            this.blunderRate = blunderRate;
            this.tolerance = tolerance;
        }
    }

    static final int MATE = 1_000_000;
    private static final int QUIESCENCE_DEPTH = 4;

    private final Random random;
    private long deadline;
    private long nodes;

    public Engine(Random random) {
        this.random = random;
    }

    public Move bestMove(GameState state, Level level) {
        List<Move> moves = state.legalMoves();
        if (moves.isEmpty()) {
            throw new IllegalStateException("둘 수 있는 수가 없습니다");
        }
        if (random.nextDouble() < level.blunderRate) {
            List<Move> real = moves.stream().filter(m -> !m.isPass()).toList();
            List<Move> pool = real.isEmpty() ? moves : real;
            return pool.get(random.nextInt(pool.size()));
        }

        nodes = 0;
        // 깊이 1은 시간과 상관없이 끝까지 읽어서 늘 둘 수를 확보한다.
        deadline = Long.MAX_VALUE;
        List<ScoredMove> scored = rootScores(state, moves, 1);
        deadline = System.currentTimeMillis() + level.timeMillis;
        for (int depth = 2; depth <= level.maxDepth; depth++) {
            try {
                scored = rootScores(state, ordered(scored), depth);
            } catch (Timeout e) {
                break;
            }
        }
        return pick(scored, level.tolerance);
    }

    /** 마지막 탐색의 노드 수. 성능 확인용. */
    public long nodes() {
        return nodes;
    }

    private record ScoredMove(Move move, int score) {
    }

    private List<ScoredMove> rootScores(GameState state, List<Move> moves, int depth) {
        List<ScoredMove> result = new ArrayList<>();
        for (Move m : order(state, moves)) {
            int score = -search(state.next(m), depth - 1, -MATE - 1, MATE + 1, 1);
            result.add(new ScoredMove(m, score));
        }
        return result;
    }

    private static List<Move> ordered(List<ScoredMove> scored) {
        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredMove::score).reversed())
                .map(ScoredMove::move)
                .toList();
    }

    private Move pick(List<ScoredMove> scored, int tolerance) {
        int best = scored.stream().mapToInt(ScoredMove::score).max().orElseThrow();
        List<Move> candidates = scored.stream()
                .filter(s -> s.score() >= best - tolerance)
                .map(ScoredMove::move)
                .toList();
        // 한수쉼은 그것만 가장 좋을 때만 둔다. 비슷한 수 사이에서 무작위로 고르다 쉬어 버리면 어색하다.
        boolean passIsOnlyBest = scored.stream().noneMatch(s -> !s.move().isPass() && s.score() == best);
        List<Move> pool = passIsOnlyBest ? candidates.stream().filter(Move::isPass).toList()
                : candidates.stream().filter(m -> !m.isPass()).toList();
        return pool.get(random.nextInt(pool.size()));
    }

    private int search(GameState state, int depth, int alpha, int beta, int ply) {
        if (++nodes % 1024 == 0 && System.currentTimeMillis() > deadline) {
            throw new Timeout();
        }
        if (state.isBikjang()) {
            return bikjangScore(state, ply);
        }
        if (depth <= 0) {
            return quiescence(state, alpha, beta, ply, QUIESCENCE_DEPTH);
        }
        List<Move> moves = state.candidateMoves();
        if (moves.isEmpty()) {
            // 장군이 아니면 한수쉼이 늘 있으므로, 수가 없으면 외통이다.
            return -MATE + ply;
        }
        for (Move m : order(state, moves)) {
            int score = -search(state.next(m), depth - 1, -beta, -alpha, ply + 1);
            if (score >= beta) {
                return score;
            }
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }

    /** 잡는 수만 더 읽어서, 기물을 주고받는 도중에 평가하는 실수를 줄인다. */
    private int quiescence(GameState state, int alpha, int beta, int ply, int depthLeft) {
        boolean inCheck = state.isCheck();
        if (!inCheck) {
            int standPat = Evaluator.evaluate(state);
            if (standPat >= beta || depthLeft == 0) {
                return standPat;
            }
            alpha = Math.max(alpha, standPat);
        }
        List<Move> moves = state.candidateMoves();
        if (moves.isEmpty()) {
            return -MATE + ply;
        }
        if (depthLeft == 0) {
            return Evaluator.evaluate(state);
        }
        for (Move m : order(state, moves)) {
            if (!inCheck && (m.isPass() || state.pieceAt(m.to()) == null)) {
                continue;
            }
            GameState child = state.next(m);
            int score = child.isBikjang()
                    ? -bikjangScore(child, ply + 1)
                    : -quiescence(child, -beta, -alpha, ply + 1, depthLeft - 1);
            if (score >= beta) {
                return score;
            }
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }

    /** 빅장은 점수 판정으로 바로 끝나므로 이기면 외통에 가까운 값, 지면 그 반대. */
    private static int bikjangScore(GameState state, int ply) {
        Side me = state.sideToMove();
        int cmp = Double.compare(state.score(me), state.score(me.opponent()));
        int win = MATE / 2 - ply;
        return cmp > 0 ? win : cmp < 0 ? -win : 0;
    }

    /** 큰 기물을 작은 기물로 잡는 수부터, 한수쉼은 맨 뒤로. */
    private static List<Move> order(GameState state, List<Move> moves) {
        List<Move> sorted = new ArrayList<>(moves);
        sorted.sort(Comparator.comparingInt((Move m) -> -orderKey(state, m)));
        return sorted;
    }

    private static int orderKey(GameState state, Move m) {
        if (m.isPass()) {
            return -1000;
        }
        Piece victim = state.pieceAt(m.to());
        if (victim == null) {
            return 0;
        }
        return victim.type().points() * 10 - state.pieceAt(m.from()).type().points();
    }

    private static final class Timeout extends RuntimeException {
        Timeout() {
            super(null, null, false, false);
        }
    }
}
