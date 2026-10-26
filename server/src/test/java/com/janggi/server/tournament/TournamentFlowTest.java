package com.janggi.server.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.janggi.server.game.GameService;
import com.janggi.server.shop.CoinService;
import com.janggi.server.shop.OwnedItemRepository;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "janggi.tournaments.tick=false")
class TournamentFlowTest {

    @Autowired
    TournamentService tournaments;

    @Autowired
    UserService users;

    @Autowired
    CoinService coins;

    @Autowired
    GameService games;

    @Autowired
    OwnedItemRepository owned;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager txm;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txm);
    }

    private User member(String name, int coinsToGive) {
        User u = users.loginWith("google", "t-" + name, null);
        if (coinsToGive > 0) {
            tx().executeWithoutResult(s -> coins.change(u.getId(), coinsToGive, CoinService.Kind.ADMIN, "test", "test"));
        }
        return u;
    }

    private TournamentService.TournamentRequest req(String title, OffsetDateTime at, Integer bonus, Tournament.Kind kind,
            Integer repeat, Tournament.Division division) {
        return new TournamentService.TournamentRequest(title, null, at, 60, 10, 1, division, false, kind, repeat, bonus,
                16, "avatar.crown");
    }

    private void advance(long id, OffsetDateTime at) {
        tx().executeWithoutResult(s -> tournaments.advance(id, at));
    }

    private TournamentService.DetailDto detail(long id) {
        return tournaments.detail(id, null);
    }

    private List<TournamentService.MatchDto> round(long id, int r) {
        return detail(id).matches().stream().filter(m -> m.round() == r).toList();
    }

    /** 대국 중인 칸마다 초를 지게 한다. 진 사람 목록을 돌려준다. */
    private List<Long> choLosesAll(long id, int r) {
        List<Long> losers = new ArrayList<>();
        for (var m : round(id, r)) {
            if (m.gameId() != null && m.winnerId() == null) {
                games.resign(m.gameId(), m.cho().id());
                losers.add(m.cho().id());
            }
        }
        return losers;
    }

    @Test
    void fivePlayersKnockoutWithByesAndPrize() {
        User admin = member("admin-a", 0);
        OffsetDateTime start = OffsetDateTime.now().plusMinutes(10);
        long id = tournaments.create(admin.getId(),
                req("주말 대회", start, 20, Tournament.Kind.SPECIAL, null, Tournament.Division.ALL)).get(0).id();
        List<User> players = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User p = member("p" + i, 3);
            tournaments.join(id, p.getId());
            players.add(p);
        }
        assertThat(coins.balance(players.get(0).getId())).isEqualTo(2);
        assertThatThrownBy(() -> tournaments.join(id, players.get(0).getId())).isInstanceOf(TournamentException.class);
        // 참가비 5닢은 소모되고, 상금은 미리 건 20닢뿐이다.
        assertThat(detail(id).summary().prizePool()).isEqualTo(20);

        // 시작: 5명이라 8칸 대진표, 1~3번 시드는 부전승이고 4번과 5번만 둔다.
        advance(id, start.plusSeconds(1));
        var r1 = round(id, 1);
        assertThat(r1).hasSize(4);
        assertThat(r1.stream().filter(m -> m.gameId() != null)).hasSize(1);
        assertThat(r1.stream().filter(m -> m.gameId() == null && m.winnerId() != null)).hasSize(3);
        assertThat(detail(id).summary().totalRounds()).isEqualTo(3);

        OffsetDateTime now = start.plusSeconds(2);
        int r = 1;
        while (true) {
            choLosesAll(id, r);
            advance(id, now);
            var d = detail(id);
            if (d.summary().status() == Tournament.Status.FINISHED) {
                break;
            }
            assertThat(d.nextRoundAt()).isNotNull();
            now = now.plusSeconds(16);
            advance(id, now);
            r++;
            assertThat(detail(id).summary().currentRound()).isEqualTo(r);
        }
        var done = detail(id);
        long winner = done.summary().winner().id();
        assertThat(done.summary().prizePool()).isEqualTo(20);
        assertThat(coins.balance(winner)).isEqualTo(2 + 20);
        assertThat(owned.existsByUserIdAndItemId(winner, "avatar.crown")).isTrue();
        assertThat(owned.existsByUserIdAndItemId(players.get(0).getId() == winner ? players.get(1).getId()
                : players.get(0).getId(), "avatar.crown")).isFalse();
        assertThat(done.entries().get(0).rank()).isEqualTo(1);
        assertThat(done.entries().get(1).rank()).isEqualTo(2);
    }

    @Test
    void noShowForfeits() {
        User admin = member("admin-b", 0);
        OffsetDateTime start = OffsetDateTime.now().plusMinutes(10);
        long id = tournaments.create(admin.getId(),
                req("노쇼", start, 0, Tournament.Kind.SPECIAL, null, Tournament.Division.ALL)).get(0).id();
        User a = member("ns-a", 1);
        User b = member("ns-b", 1);
        tournaments.join(id, a.getId());
        tournaments.join(id, b.getId());
        advance(id, start.plusSeconds(1));
        var m = round(id, 1).get(0);
        // 초가 1분 넘게 첫 수를 두지 않으면 초의 기권패.
        advance(id, OffsetDateTime.now().plusSeconds(61));
        advance(id, OffsetDateTime.now().plusSeconds(62));
        var d = detail(id);
        assertThat(d.summary().status()).isEqualTo(Tournament.Status.FINISHED);
        assertThat(d.summary().winner().id()).isEqualTo(m.han().id());
    }

    @Test
    void tooFewPlayersCancelsAndRegularRepeats() {
        User admin = member("admin-c", 0);
        OffsetDateTime start = OffsetDateTime.now().plusMinutes(10);
        long id = tournaments.create(admin.getId(),
                req("매주 리그", start, 0, Tournament.Kind.REGULAR, 7, Tournament.Division.ALL)).get(0).id();
        User lone = member("lone", 1);
        tournaments.join(id, lone.getId());
        assertThat(coins.balance(lone.getId())).isZero();
        advance(id, start.plusSeconds(1));
        assertThat(detail(id).summary().status()).isEqualTo(Tournament.Status.CANCELLED);
        assertThat(coins.balance(lone.getId())).isEqualTo(1);
        Integer next = jdbc.queryForObject("SELECT COUNT(*) FROM tournaments WHERE title = '매주 리그'"
                + " AND status = 'SCHEDULED'", Integer.class);
        assertThat(next).isEqualTo(1);
    }

    @Test
    void divisionsAndBoth() {
        User admin = member("admin-d", 0);
        var made = tournaments.create(admin.getId(), new TournamentService.TournamentRequest("밤 리그", null,
                OffsetDateTime.now().plusHours(1), null, null, null, null, true, null, null, null, null, null));
        assertThat(made).extracting(TournamentService.SummaryDto::title)
                .containsExactly("밤 리그 유단자 리그", "밤 리그 급 리그");
        User kyu = member("kyu", 5);
        long danId = made.get(0).id();
        assertThatThrownBy(() -> tournaments.join(danId, kyu.getId())).hasMessageContaining("유단자");
        tournaments.join(made.get(1).id(), kyu.getId());
    }

    @Test
    void memberHostsAndCancelsWithRefund() {
        User host = member("host", 10);
        var d = tournaments.host(host.getId(), new TournamentService.TournamentRequest("내 대회", null,
                OffsetDateTime.now().plusHours(2), 60, 5, 2, Tournament.Division.DAN, true, Tournament.Kind.REGULAR,
                7, 6, 8, "avatar.crown"));
        // 회원 대회는 리그·반복을 쓰지 않는다.
        assertThat(d.summary().kind()).isEqualTo(Tournament.Kind.SPECIAL);
        assertThat(d.summary().division()).isEqualTo(Tournament.Division.ALL);
        assertThat(d.summary().maxPlayers()).isEqualTo(8);
        // 보상 아바타는 운영 대회에만 있다(개인 대회로 금관을 찍어 내지 못하게).
        assertThat(d.summary().prizeItem()).isNull();
        assertThat(d.summary().host().id()).isEqualTo(host.getId());
        assertThat(coins.balance(host.getId())).isEqualTo(4);
        assertThatThrownBy(() -> tournaments.host(host.getId(), req("또", OffsetDateTime.now().plusHours(3), 0, null,
                null, null))).hasMessageContaining("이미 연 대회");
        User guest = member("guest", 2);
        tournaments.join(d.summary().id(), guest.getId());
        assertThatThrownBy(() -> tournaments.cancelHosted(d.summary().id(), guest.getId()))
                .isInstanceOf(TournamentException.class);
        tournaments.cancelHosted(d.summary().id(), host.getId());
        assertThat(coins.balance(host.getId())).isEqualTo(10);
        assertThat(coins.balance(guest.getId())).isEqualTo(2);
    }
}
