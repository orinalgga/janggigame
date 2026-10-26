package com.janggi.server.tournament;

import com.janggi.core.Formation;
import com.janggi.core.GameResult;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import com.janggi.server.match.MatchService;
import com.janggi.server.record.GameRecord;
import com.janggi.server.record.GameRecordRepository;
import com.janggi.server.shop.CoinService;
import com.janggi.server.shop.OwnedItem;
import com.janggi.server.shop.OwnedItemRepository;
import com.janggi.server.shop.ShopException;
import com.janggi.server.shop.ShopItem;
import com.janggi.server.user.Grade;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 대회(토너먼트). 시작 시각이 되면 레이팅 순으로 시드를 정해 대진표를 짜고, 지면 탈락한다.
 * 한 라운드가 모두 끝나면 잠깐 쉬었다가 다음 라운드를 짝짓고, 결승이 끝나면 모인 참가비를 우승자에게 준다.
 * 진행 상태는 모두 DB에 있어서 서버가 다시 켜져도 이어서 진행한다.
 */
@Service
public class TournamentService {

    private static final Logger log = LoggerFactory.getLogger(TournamentService.class);

    /** 짝지어 준 사람에게 다음 대국을 알리는 곳(/user/queue/tournament). */
    public static final String DESTINATION = "/queue/tournament";
    /** 대국이 시작되고 이 시간 안에 첫 수를 두지 않으면 기권으로 본다. */
    static final Duration NO_SHOW = Duration.ofSeconds(60);
    /** 라운드 사이 쉬는 시간. */
    static final Duration BREAK = Duration.ofSeconds(15);
    public static final int MAX_PLAYERS = 16;
    private static final Formation FORMATION = Formation.MA_SANG_MA_SANG;
    private static final List<Tournament.Status> OPEN = List.of(Tournament.Status.SCHEDULED,
            Tournament.Status.RUNNING);

    public record PlayerRef(long id, String nickname, String avatar, String grade, int rating, Integer seed) {
    }

    /** 대국이 끝났으면 winnerId가 있다. 한쪽이 비면 부전승. */
    public record MatchDto(int round, int slot, PlayerRef cho, PlayerRef han, String gameId, Long winnerId) {
    }

    /** rank는 끝난 대회에서만(우승 1, 준우승 2, 4강 3 ...). out은 탈락한 라운드. */
    public record EntryDto(PlayerRef player, Integer out, Integer rank) {
    }

    /** host는 회원이 연 개인 대회일 때 연 사람. prizePool은 우승 상금으로, 운영자나 연 사람이 미리 건 bonusPrize다. 참가비는 참가 비용으로 소모되고 상금에 들어가지 않는다. */
    public record SummaryDto(long id, String title, OffsetDateTime startsAt, Tournament.Status status,
            Tournament.Division division, Tournament.Kind kind, int repeatDays, PlayerRef host, int bonusPrize,
            int baseSeconds, int incrementSeconds, int entryFee, int maxPlayers, int players, int prizePool,
            int currentRound, int totalRounds, PlayerRef winner, boolean joined, String prizeItem) {
    }

    /** myGameId는 내가 지금 둬야 하는 대회 대국. */
    public record DetailDto(SummaryDto summary, String description, List<EntryDto> entries, List<MatchDto> matches,
            OffsetDateTime nextRoundAt, String myGameId, boolean eliminated) {
    }

    /** 다음 대국이 짝지어졌다는 알림. */
    public record Paired(long tournamentId, int round, String gameId) {
    }

    /**
     * both가 참이면(새로 만들 때) 유단자 리그와 급 리그를 하나씩 만든다. repeatDays는 정기 리그의 반복 간격(1: 매일, 7: 매주).
     * bonusPrize는 우승 상금(엽전). prizeItem은 우승자에게 줄 보상 아바타(ShopItem.reward)로, 비우면 주지 않는다.
     * 회원이 여는 개인 대회는 kind, division, repeatDays, both, prizeItem을 쓰지 않는다.
     */
    public record TournamentRequest(String title, String description, OffsetDateTime startsAt, Integer baseSeconds,
            Integer incrementSeconds, Integer entryFee, Tournament.Division division, Boolean both,
            Tournament.Kind kind, Integer repeatDays, Integer bonusPrize, Integer maxPlayers, String prizeItem) {
    }

    /** 개인 대회 상금과 참가비 상한. */
    static final int HOST_MAX_FEE = 10;
    static final int HOST_MAX_BONUS = 1000;
    private static final List<Integer> SIZES = List.of(4, 8, 16);

    private final TournamentRepository tournaments;
    private final TournamentEntryRepository entries;
    private final TournamentMatchRepository matches;
    private final UserRepository users;
    private final GameRecordRepository gameRecords;
    private final GameService games;
    private final MatchService queue;
    private final CoinService coins;
    private final OwnedItemRepository owned;
    private final SimpMessagingTemplate messaging;
    private final TransactionTemplate tx;
    private final Random random = new Random();

    /** 테스트는 시각을 정해 advance를 직접 부르므로, 실제 시각으로 도는 tick이 끼어들지 않게 끈다. */
    @Value("${janggi.tournaments.tick:true}")
    private boolean ticking = true;

    public TournamentService(TournamentRepository tournaments, TournamentEntryRepository entries,
            TournamentMatchRepository matches, UserRepository users, GameRecordRepository gameRecords,
            GameService games, MatchService queue, CoinService coins, OwnedItemRepository owned,
            SimpMessagingTemplate messaging, PlatformTransactionManager transactions) {
        this.tournaments = tournaments;
        this.entries = entries;
        this.matches = matches;
        this.users = users;
        this.gameRecords = gameRecords;
        this.games = games;
        this.queue = queue;
        this.coins = coins;
        this.owned = owned;
        this.messaging = messaging;
        this.tx = new TransactionTemplate(transactions);
    }

    // ---- 보기 ----

    /** 로비 목록: 신청 중·진행 중인 대회와 하루 안에 끝난 대회. */
    @Transactional(readOnly = true)
    public List<SummaryDto> list(Long userId) {
        return tournaments.findTop20ByStatusInOrFinishedAtAfterOrderByStartsAtAsc(OPEN,
                        OffsetDateTime.now().minusDays(1))
                .stream().map(t -> summary(t, userId)).toList();
    }

    @Transactional(readOnly = true)
    public List<SummaryDto> all() {
        return tournaments.findTop50ByOrderByStartsAtDesc().stream().map(t -> summary(t, null)).toList();
    }

    @Transactional(readOnly = true)
    public DetailDto detail(long id, Long userId) {
        Tournament t = find(id);
        List<TournamentEntry> list = entries.findByTournamentIdOrderByJoinedAtAscIdAsc(id);
        List<TournamentMatch> ms = matches.findByTournamentIdOrderByRoundAscSlotAsc(id);
        Map<Long, User> people = people(list);
        Map<Long, Integer> seeds = list.stream().filter(e -> e.getSeed() != null)
                .collect(Collectors.toMap(TournamentEntry::getUserId, TournamentEntry::getSeed));
        Function<Long, PlayerRef> ref = uid -> uid == null ? null : ref(people.get(uid), seeds.get(uid));
        boolean finished = t.getStatus() == Tournament.Status.FINISHED;
        int rounds = totalRounds(t, list);
        List<EntryDto> es = list.stream()
                .sorted(Comparator.comparing((TournamentEntry e) -> Objects.equals(e.getUserId(), t.getWinnerId()) ? 0 : 1)
                        .thenComparing(e -> e.getEliminatedRound() == null ? Integer.MAX_VALUE : e.getEliminatedRound(),
                                Comparator.reverseOrder())
                        .thenComparing(e -> e.getSeed() == null ? Integer.MAX_VALUE : e.getSeed())
                        .thenComparing(TournamentEntry::getJoinedAt))
                .map(e -> new EntryDto(ref.apply(e.getUserId()), e.getEliminatedRound(),
                        !finished ? null : Objects.equals(e.getUserId(), t.getWinnerId()) ? 1
                                : e.getEliminatedRound() == null ? null : rank(rounds, e.getEliminatedRound())))
                .toList();
        List<MatchDto> mds = ms.stream()
                .map(m -> new MatchDto(m.getRound(), m.getSlot(), ref.apply(m.getChoUserId()),
                        ref.apply(m.getHanUserId()), m.getGameId() == null ? null : m.getGameId().toString(),
                        m.getWinnerId()))
                .toList();
        String myGame = userId == null ? null : ms.stream()
                .filter(m -> m.getRound() == t.getCurrentRound() && m.getWinnerId() == null && m.getGameId() != null
                        && (userId.equals(m.getChoUserId()) || userId.equals(m.getHanUserId())))
                .map(m -> m.getGameId().toString()).findFirst().orElse(null);
        boolean out = userId != null && list.stream()
                .anyMatch(e -> e.getUserId() == userId && e.getEliminatedRound() != null);
        return new DetailDto(summary(t, userId, list, people), t.getDescription(), es, mds, t.getNextRoundAt(),
                myGame, out);
    }

    /** 2^n강에서 r라운드에 진 사람의 순위. 결승에서 지면 2, 4강이면 3, 8강이면 5 ... */
    static int rank(int rounds, int eliminatedRound) {
        return (1 << (rounds - eliminatedRound)) + 1;
    }

    // ---- 참가 ----

    @Transactional
    public DetailDto join(long id, long userId) {
        Tournament t = find(id);
        if (t.getStatus() != Tournament.Status.SCHEDULED || !OffsetDateTime.now().isBefore(t.getStartsAt())) {
            throw new TournamentException("참가 신청이 끝난 대회예요");
        }
        if (entries.findByTournamentIdAndUserId(id, userId).isPresent()) {
            throw new TournamentException("이미 참가 신청했어요");
        }
        int rating = users.findById(userId).map(User::getRating).orElse(0);
        if (t.getDivision() == Tournament.Division.DAN && !Grade.dan(rating)) {
            throw new TournamentException("유단자(초단 이상) 리그예요");
        }
        if (t.getDivision() == Tournament.Division.KYU && Grade.dan(rating)) {
            throw new TournamentException("급 리그예요. 유단자 리그에 참가해 주세요");
        }
        if (entries.countByTournamentId(id) >= t.getMaxPlayers()) {
            throw new TournamentException("정원(" + t.getMaxPlayers() + "명)이 다 찼어요");
        }
        int free = 0;
        if (t.getEntryFee() > 0) {
            try {
                free = coins.payEntry(userId, t.getEntryFee(), ref(t), t.getTitle() + " 참가비");
            } catch (ShopException e) {
                throw new TournamentException("참가비 엽전이 모자라요");
            }
        }
        entries.saveAndFlush(new TournamentEntry(id, userId, t.getEntryFee() - free, free, OffsetDateTime.now()));
        return detail(id, userId);
    }

    /** 시작 전이면 신청을 취소하고 참가비를 돌려준다. 진행 중이면 기권한다(참가비는 돌려주지 않는다). */
    @Transactional
    public DetailDto leave(long id, long userId) {
        Tournament t = find(id);
        TournamentEntry e = entries.findByTournamentIdAndUserId(id, userId)
                .orElseThrow(() -> new TournamentException("참가 신청하지 않은 대회예요"));
        switch (t.getStatus()) {
            case SCHEDULED -> {
                refund(t, e);
                entries.delete(e);
            }
            case RUNNING -> {
                if (e.getEliminatedRound() == null) {
                    e.eliminated(t.getCurrentRound());
                }
                for (TournamentMatch m : matches.findByTournamentIdAndRoundOrderBySlotAsc(id, t.getCurrentRound())) {
                    if (m.getWinnerId() == null && m.getGameId() != null
                            && (Objects.equals(m.getChoUserId(), userId) || Objects.equals(m.getHanUserId(), userId))) {
                        resignQuietly(m.getGameId().toString(), userId);
                    }
                }
            }
            default -> throw new TournamentException("이미 끝난 대회예요");
        }
        return detail(id, userId);
    }

    // ---- 관리자 ----

    @Transactional
    public List<SummaryDto> create(long adminId, TournamentRequest req) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Tournament.Division> divisions = Boolean.TRUE.equals(req.both())
                ? List.of(Tournament.Division.DAN, Tournament.Division.KYU)
                : List.of(req.division() == null ? Tournament.Division.ALL : req.division());
        List<SummaryDto> made = new ArrayList<>();
        for (Tournament.Division d : divisions) {
            Tournament t = new Tournament(adminId, now);
            apply(t, req, now, d, divisions.size() > 1, false);
            made.add(summary(tournaments.save(t), null));
        }
        return made;
    }

    /** 회원이 여는 개인 특별 리그. 상금을 걸면 그만큼 엽전이 빠지고, 대회가 취소되면 돌려받는다. */
    @Transactional
    public DetailDto host(long userId, TournamentRequest req) {
        if (tournaments.existsByHostIdAndStatusIn(userId, OPEN)) {
            throw new TournamentException("이미 연 대회가 있어요. 그 대회가 끝난 뒤에 새로 열 수 있어요");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Tournament t = new Tournament(userId, now);
        apply(t, req, now, Tournament.Division.ALL, false, true);
        tournaments.saveAndFlush(t);
        int bonus = t.getBonusPrize();
        int free = 0;
        if (bonus > 0) {
            try {
                free = coins.payEntry(userId, bonus, ref(t), t.getTitle() + " 상금 걸기");
            } catch (ShopException e) {
                throw new TournamentException("상금으로 걸 엽전이 모자라요");
            }
        }
        t.hostedBy(userId, bonus - free, free);
        return detail(t.getId(), userId);
    }

    /** 개인 대회를 연 사람이 시작 전에 취소한다. 참가비와 건 상금을 모두 돌려준다. */
    @Transactional
    public DetailDto cancelHosted(long id, long userId) {
        Tournament t = find(id);
        if (!Objects.equals(t.getHostId(), userId)) {
            throw new TournamentException("내가 연 대회만 취소할 수 있어요");
        }
        if (t.getStatus() != Tournament.Status.SCHEDULED) {
            throw new TournamentException("시작한 대회는 취소할 수 없어요");
        }
        cancelAndRefund(t);
        return detail(id, userId);
    }

    @Transactional
    public SummaryDto update(long id, TournamentRequest req) {
        Tournament t = find(id);
        if (t.getStatus() != Tournament.Status.SCHEDULED) {
            throw new TournamentException("시작한 대회는 고칠 수 없어요");
        }
        if (req.entryFee() != null && req.entryFee() != t.getEntryFee() && entries.countByTournamentId(id) > 0) {
            throw new TournamentException("참가자가 있으면 참가비를 바꿀 수 없어요");
        }
        if (t.getHostId() != null) {
            throw new TournamentException("회원이 연 대회는 고칠 수 없어요. 필요하면 취소해 주세요");
        }
        apply(t, req, OffsetDateTime.now(), req.division() == null ? t.getDivision() : req.division(), false, false);
        return summary(t, null);
    }

    /** 대회를 취소하고 모든 참가비를 돌려준다. 진행 중인 대국은 그냥 온라인 대국으로 끝까지 둔다. */
    @Transactional
    public SummaryDto cancel(long id) {
        Tournament t = find(id);
        if (t.getStatus() != Tournament.Status.SCHEDULED && t.getStatus() != Tournament.Status.RUNNING) {
            throw new TournamentException("이미 끝난 대회예요");
        }
        cancelAndRefund(t);
        return summary(t, null);
    }

    private void apply(Tournament t, TournamentRequest req, OffsetDateTime now, Tournament.Division division,
            boolean suffix, boolean hosted) {
        String title = req.title() == null ? "" : req.title().strip();
        if (suffix && !title.isEmpty()) {
            title += division == Tournament.Division.DAN ? " 유단자 리그" : " 급 리그";
        }
        if (title.isEmpty() || title.length() > 60) {
            throw new TournamentException("대회 이름은 1~60자로 적어 주세요");
        }
        String desc = req.description() == null || req.description().isBlank() ? null : req.description().strip();
        if (desc != null && desc.length() > 1000) {
            throw new TournamentException("소개는 1000자까지예요");
        }
        if (req.startsAt() == null || !req.startsAt().isAfter(now)) {
            throw new TournamentException("시작 시각은 지금보다 뒤여야 해요");
        }
        int base = req.baseSeconds() == null ? 60 : req.baseSeconds();
        int inc = req.incrementSeconds() == null ? 10 : req.incrementSeconds();
        int fee = req.entryFee() == null ? 1 : req.entryFee();
        if (base < 30 || base > 1800 || inc < 0 || inc > 60) {
            throw new TournamentException("기본 시간은 30초~30분, 수마다 더하는 시간은 0~60초로 정해 주세요");
        }
        int maxFee = hosted ? HOST_MAX_FEE : 100;
        if (fee < 0 || fee > maxFee) {
            throw new TournamentException("참가비는 0~" + maxFee + "닢으로 정해 주세요");
        }
        int size = req.maxPlayers() == null ? MAX_PLAYERS : req.maxPlayers();
        if (!SIZES.contains(size)) {
            throw new TournamentException("정원은 4, 8, 16명 중에서 골라 주세요");
        }
        int bonus = req.bonusPrize() == null ? 0 : req.bonusPrize();
        int maxBonus = hosted ? HOST_MAX_BONUS : 100_000;
        if (bonus < 0 || bonus > maxBonus) {
            throw new TournamentException("얹는 상금은 0~" + maxBonus + "닢으로 정해 주세요");
        }
        Tournament.Kind kind = hosted || req.kind() == null ? Tournament.Kind.SPECIAL : req.kind();
        int repeat = kind == Tournament.Kind.REGULAR ? (req.repeatDays() == null ? 7 : req.repeatDays()) : 0;
        if (kind == Tournament.Kind.REGULAR && repeat != 1 && repeat != 7) {
            throw new TournamentException("정기 리그는 매일(1) 또는 매주(7) 반복으로 정해 주세요");
        }
        if (hosted && req.startsAt().isAfter(now.plusDays(7))) {
            throw new TournamentException("개인 대회는 7일 안에 시작해야 해요");
        }
        String prizeItem = req.prizeItem() == null || req.prizeItem().isBlank() || hosted ? null : req.prizeItem();
        if (prizeItem != null && ShopItem.byId(prizeItem).filter(ShopItem::reward).isEmpty()) {
            throw new TournamentException("우승 보상은 대회 보상 아바타 중에서 골라 주세요");
        }
        t.edit(title, desc, req.startsAt(), base, inc, fee, size, division, kind, repeat, bonus);
        t.prizeItem(prizeItem);
    }

    // ---- 진행 ----

    /** 신청 중·진행 중인 대회를 하나씩 따로 트랜잭션으로 진행한다. 한 대회에서 난 오류가 다른 대회를 막지 않는다. */
    @Scheduled(fixedDelay = 2000)
    public void tick() {
        if (!ticking) {
            return;
        }
        for (Tournament t : tournaments.findByStatusIn(OPEN)) {
            try {
                tx.executeWithoutResult(s -> advance(t.getId(), OffsetDateTime.now()));
            } catch (RuntimeException e) {
                log.warn("대회 {} 진행 중 오류", t.getId(), e);
            }
        }
    }

    /** 대회 하나를 한 걸음 진행한다. 테스트에서 시각을 정해 부를 수 있다. */
    void advance(long id, OffsetDateTime now) {
        Tournament t = find(id);
        if (t.getStatus() == Tournament.Status.SCHEDULED && !now.isBefore(t.getStartsAt())) {
            begin(t, now);
        } else if (t.getStatus() == Tournament.Status.RUNNING) {
            play(t, now);
        }
    }

    private void begin(Tournament t, OffsetDateTime now) {
        // 정기 리그는 이번 회차가 열리든 사람이 모자라 취소되든 다음 회차를 만들어 둔다.
        if (t.getKind() == Tournament.Kind.REGULAR && t.getRepeatDays() > 0) {
            tournaments.save(t.nextOccurrence(now));
        }
        List<TournamentEntry> list = entries.findByTournamentIdOrderByJoinedAtAscIdAsc(t.getId());
        if (list.size() < 2) {
            cancelAndRefund(t);
            return;
        }
        Map<Long, User> people = people(list);
        List<TournamentEntry> seeded = list.stream()
                .sorted(Comparator.comparing((TournamentEntry e) -> people.get(e.getUserId()).getRating())
                        .reversed().thenComparing(TournamentEntry::getJoinedAt))
                .toList();
        for (int i = 0; i < seeded.size(); i++) {
            seeded.get(i).seed(i + 1);
        }
        int size = Bracket.size(seeded.size());
        List<Integer> order = Bracket.order(size);
        List<Long[]> pairs = new ArrayList<>();
        for (int k = 0; k < size / 2; k++) {
            pairs.add(new Long[] {userAt(seeded, order.get(2 * k)), userAt(seeded, order.get(2 * k + 1))});
        }
        t.start(now);
        createRound(t, 1, pairs, now);
    }

    private static Long userAt(List<TournamentEntry> seeded, int seed) {
        return seed <= seeded.size() ? seeded.get(seed - 1).getUserId() : null;
    }

    private void play(Tournament t, OffsetDateTime now) {
        int round = t.getCurrentRound();
        List<TournamentMatch> current = matches.findByTournamentIdAndRoundOrderBySlotAsc(t.getId(), round);
        Map<Long, TournamentEntry> byUser = entries.findByTournamentIdOrderByJoinedAtAscIdAsc(t.getId()).stream()
                .collect(Collectors.toMap(TournamentEntry::getUserId, e -> e));
        for (TournamentMatch m : current) {
            if (m.getWinnerId() == null && m.getGameId() != null) {
                settle(m, byUser, now);
            }
        }
        boolean allDone = current.stream().allMatch(m -> m.getWinnerId() != null || m.bye() && none(m));
        if (!allDone) {
            return;
        }
        if (current.size() == 1) {
            finish(t, current.get(0).getWinnerId(), byUser.size(), now);
            return;
        }
        if (t.getNextRoundAt() == null) {
            t.roundDone(now.plus(BREAK));
            return;
        }
        if (now.isBefore(t.getNextRoundAt())) {
            return;
        }
        List<Long[]> pairs = new ArrayList<>();
        for (int k = 0; k < current.size() / 2; k++) {
            pairs.add(new Long[] {alive(current.get(2 * k).getWinnerId(), byUser),
                    alive(current.get(2 * k + 1).getWinnerId(), byUser)});
        }
        t.nextRound();
        createRound(t, t.getCurrentRound(), pairs, now);
    }

    private static boolean none(TournamentMatch m) {
        return m.getChoUserId() == null && m.getHanUserId() == null;
    }

    /** 다음 라운드에 나갈 사람. 쉬는 동안 기권했으면 빈자리로 본다. */
    private static Long alive(Long userId, Map<Long, TournamentEntry> byUser) {
        if (userId == null) {
            return null;
        }
        TournamentEntry e = byUser.get(userId);
        return e == null || e.getEliminatedRound() != null ? null : userId;
    }

    /** 끝난 대국의 승자를 정하고, 안 온 사람은 기권시킨다. */
    private void settle(TournamentMatch m, Map<Long, TournamentEntry> byUser, OffsetDateTime now) {
        GameRecord g = gameRecords.findById(m.getGameId()).orElse(null);
        if (g == null) {
            return;
        }
        if (g.getResult() == GameResult.ONGOING) {
            if (g.getMoveCount() == 0 && g.getStartedAt().plus(NO_SHOW).isBefore(now)) {
                resignQuietly(g.getId().toString(), m.getChoUserId());
            } else if (g.getMoveCount() == 1 && g.getClockUpdatedAt() != null
                    && g.getClockUpdatedAt().plus(NO_SHOW).isBefore(now)) {
                resignQuietly(g.getId().toString(), m.getHanUserId());
            }
            return;
        }
        long winner = switch (g.getResult()) {
            case CHO_WIN -> m.getChoUserId();
            case HAN_WIN -> m.getHanUserId();
            // 무승부면 다시 두지 않고 남은 기물 점수로 가린다. 한에 덤 1.5점이 있어 같을 수 없다.
            default -> {
                GameDto end = games.get(g.getId().toString());
                yield end.choScore() > end.hanScore() ? m.getChoUserId() : m.getHanUserId();
            }
        };
        m.won(winner);
        long loser = winner == m.getChoUserId() ? m.getHanUserId() : m.getChoUserId();
        TournamentEntry le = byUser.get(loser);
        if (le != null && le.getEliminatedRound() == null) {
            le.eliminated(m.getRound());
        }
    }

    private void createRound(Tournament t, int round, List<Long[]> pairs, OffsetDateTime now) {
        for (int slot = 0; slot < pairs.size(); slot++) {
            Long a = pairs.get(slot)[0];
            Long b = pairs.get(slot)[1];
            boolean swap = a != null && b != null && random.nextBoolean();
            TournamentMatch m = new TournamentMatch(t.getId(), round, slot, swap ? b : a, swap ? a : b, now);
            if (a == null || b == null) {
                Long only = a != null ? a : b;
                if (only != null) {
                    m.won(only);
                }
                matches.save(m);
                continue;
            }
            queue.leave(a);
            queue.leave(b);
            GameDto game = games.createOnline(m.getChoUserId(), FORMATION, m.getHanUserId(), FORMATION,
                    t.getBaseSeconds(), t.getIncrementSeconds());
            m.game(java.util.UUID.fromString(game.id()));
            matches.save(m);
            Paired paired = new Paired(t.getId(), round, game.id());
            messaging.convertAndSendToUser(a.toString(), DESTINATION, paired);
            messaging.convertAndSendToUser(b.toString(), DESTINATION, paired);
        }
    }

    private void finish(Tournament t, Long winnerId, int players, OffsetDateTime now) {
        // 참가비는 판돈이 아니라 소모되는 참가 비용이다. 상금은 미리 정해 둔 금액만 준다.
        int prize = t.getBonusPrize();
        if (winnerId != null && prize > 0) {
            coins.change(winnerId, prize, CoinService.Kind.PRIZE, ref(t), t.getTitle() + " 우승 상금");
        }
        // 보상 아바타는 운영 대회에만 있다. 이미 가지고 있으면 그대로 둔다.
        String item = t.getPrizeItem();
        if (winnerId != null && item != null && !owned.existsByUserIdAndItemId(winnerId, item)) {
            owned.save(new OwnedItem(winnerId, item, now));
        }
        t.finish(winnerId, winnerId == null ? 0 : prize, now);
    }

    private void cancelAndRefund(Tournament t) {
        for (TournamentEntry e : entries.findByTournamentIdOrderByJoinedAtAscIdAsc(t.getId())) {
            refund(t, e);
        }
        if (t.getHostId() != null && t.getBonusPaid() + t.getBonusFree() > 0) {
            coins.refund(t.getHostId(), t.getBonusPaid(), t.getBonusFree(), ref(t), t.getTitle() + " 건 상금 환불");
        }
        t.cancel(OffsetDateTime.now());
    }

    private void refund(Tournament t, TournamentEntry e) {
        if (e.getFeePaid() + e.getFeeFree() > 0) {
            coins.refund(e.getUserId(), e.getFeePaid(), e.getFeeFree(), ref(t), t.getTitle() + " 참가비 환불");
        }
    }

    private void resignQuietly(String gameId, Long userId) {
        try {
            games.resign(gameId, userId);
        } catch (RuntimeException e) {
            log.debug("대회 대국 {} 기권 처리 실패", gameId, e);
        }
    }

    // ---- 도우미 ----

    private Tournament find(long id) {
        return tournaments.findById(id).orElseThrow(() -> new TournamentException("대회를 찾을 수 없어요"));
    }

    private static String ref(Tournament t) {
        return "tournament:" + t.getId();
    }

    private Map<Long, User> people(List<TournamentEntry> list) {
        Map<Long, User> map = new HashMap<>();
        users.findAllById(list.stream().map(TournamentEntry::getUserId).toList()).forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private static PlayerRef ref(User u, Integer seed) {
        return u == null ? null : new PlayerRef(u.getId(), u.getNickname(), u.getAvatar(), Grade.name(u.getRating()), u.getRating(), seed);
    }

    private static int totalRounds(Tournament t, List<TournamentEntry> list) {
        int n = t.getStatus() == Tournament.Status.SCHEDULED ? Math.max(list.size(), 2)
                : (int) list.stream().filter(e -> e.getSeed() != null).count();
        return Bracket.rounds(Bracket.size(Math.max(n, 2)));
    }

    private SummaryDto summary(Tournament t, Long userId) {
        List<TournamentEntry> list = entries.findByTournamentIdOrderByJoinedAtAscIdAsc(t.getId());
        return summary(t, userId, list, t.getWinnerId() == null ? Map.of() : people(list));
    }

    private SummaryDto summary(Tournament t, Long userId, List<TournamentEntry> list, Map<Long, User> people) {
        boolean joined = userId != null && list.stream().anyMatch(e -> e.getUserId() == userId);
        PlayerRef winner = t.getWinnerId() == null ? null : ref(people.get(t.getWinnerId()), null);
        int pool = t.getStatus() == Tournament.Status.FINISHED ? t.getPrize() : t.getBonusPrize();
        PlayerRef host = t.getHostId() == null ? null
                : ref(people.containsKey(t.getHostId()) ? people.get(t.getHostId())
                        : users.findById(t.getHostId()).orElse(null), null);
        return new SummaryDto(t.getId(), t.getTitle(), t.getStartsAt(), t.getStatus(), t.getDivision(), t.getKind(),
                t.getRepeatDays(), host, t.getBonusPrize(), t.getBaseSeconds(),
                t.getIncrementSeconds(), t.getEntryFee(), t.getMaxPlayers(), list.size(), pool, t.getCurrentRound(),
                totalRounds(t, list), winner, joined, t.getPrizeItem());
    }
}
