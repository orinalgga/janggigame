package com.janggi.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.server.auth.JwtService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "janggi.admin-emails=notice-boss@example.com")
@AutoConfigureMockMvc
class NoticeRankingTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private String bearer(User u) {
        return "Bearer " + jwt.issue(u.getId());
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void adminPostsNoticeAndEveryoneSeesIt() throws Exception {
        String boss = bearer(users.loginWith("google", "notice-1", "notice-boss@example.com"));
        String member = bearer(users.loginWith("google", "notice-2", "notice-member@example.com"));

        mvc.perform(post("/api/admin/notices").header("Authorization", member)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"body\":\"y\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/notices").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"body\":\"y\"}"))
                .andExpect(status().isBadRequest());

        long id = body(mvc.perform(post("/api/admin/notices").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"점검 안내\",\"body\":\"오늘 밤 서버 점검\\n10분이면 끝나요\"}"))
                .andExpect(status().isOk())).get("id").asLong();
        String later = OffsetDateTime.now().plusDays(1).toString();
        long future = body(mvc.perform(post("/api/admin/notices").header("Authorization", boss)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"내일 공지\",\"body\":\"아직 안 보여요\",\"startsAt\":\"" + later + "\"}"))).get("id")
                .asLong();

        // 로그인하지 않아도 지금 보이는 공지만 받는다.
        JsonNode showing = body(mvc.perform(get("/api/notices")).andExpect(status().isOk()));
        assertThat(showing.findValuesAsText("title")).contains("점검 안내").doesNotContain("내일 공지");

        mvc.perform(put("/api/admin/notices/" + id).header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"점검 안내\",\"body\":\"끝났어요\",\"active\":false}"))
                .andExpect(jsonPath("$.active").value(false));
        assertThat(body(mvc.perform(get("/api/notices"))).findValuesAsText("title")).doesNotContain("점검 안내");

        mvc.perform(delete("/api/admin/notices/" + future).header("Authorization", boss)).andExpect(status().isOk());
        assertThat(body(mvc.perform(get("/api/admin/notices").header("Authorization", boss)))
                .findValuesAsText("title")).doesNotContain("내일 공지");
        JsonNode logs = body(mvc.perform(get("/api/admin/logs").header("Authorization", boss)));
        assertThat(logs.get(0).get("action").asText()).isEqualTo("NOTICE_DELETE");
        assertThat(logs.get(0).get("detail").asText()).isEqualTo("내일 공지");
    }

    private void game(User cho, User han, String result, int choDelta, OffsetDateTime endedAt) {
        jdbc.update("INSERT INTO games (id, cho_formation, han_formation, result, end_reason, move_count, started_at,"
                + " ended_at, online, cho_user_id, han_user_id, cho_rating_change, han_rating_change)"
                + " VALUES (?, 'SANG_MA_SANG_MA', 'SANG_MA_SANG_MA', ?, 'RESIGN', 30, ?, ?, TRUE, ?, ?, ?, ?)",
                UUID.randomUUID(), result, endedAt.minusMinutes(20), endedAt, cho.getId(), han.getId(), choDelta,
                -choDelta);
    }

    private static JsonNode find(JsonNode rows, long userId) {
        for (JsonNode r : rows) {
            if (r.get("userId").asLong() == userId) {
                return r;
            }
        }
        return null;
    }

    @Test
    void weeklyRankingAddsUpRatingChanges() throws Exception {
        User a = users.loginWith("google", "rank-a", null);
        User b = users.loginWith("google", "rank-b", null);
        User c = users.loginWith("google", "rank-c", null);
        OffsetDateTime now = OffsetDateTime.now();
        game(a, b, "CHO_WIN", 16, now);
        game(a, b, "CHO_WIN", 15, now);
        game(b, a, "CHO_WIN", 17, now); // b 승
        game(a, c, "CHO_WIN", 20, now);
        // 지난주 대국은 이번 주에 들어가지 않는다.
        OffsetDateTime lastWeek = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime().minusHours(1);
        game(c, a, "CHO_WIN", 30, lastWeek);

        JsonNode week = body(mvc.perform(get("/api/rankings").param("period", "week").header("Authorization", bearer(a)))
                .andExpect(status().isOk()));
        assertThat(week.get("minGames").asInt()).isEqualTo(3);
        JsonNode rowA = find(week.get("rows"), a.getId());
        assertThat(rowA).isNotNull();
        assertThat(rowA.get("points").asInt()).isEqualTo(16 + 15 - 17 + 20);
        assertThat(rowA.get("games").asInt()).isEqualTo(4);
        assertThat(rowA.get("wins").asInt()).isEqualTo(3);
        assertThat(rowA.get("losses").asInt()).isEqualTo(1);
        assertThat(week.get("me").get("rank").asInt()).isEqualTo(rowA.get("rank").asInt());
        // c는 이번 주 1판뿐이라 순위에는 없고, 자기 줄로만 받는다.
        assertThat(find(week.get("rows"), c.getId())).isNull();
        JsonNode cWeek = body(mvc.perform(get("/api/rankings").param("period", "week").header("Authorization", bearer(c))));
        assertThat(cWeek.get("me").get("games").asInt()).isEqualTo(1);
        assertThat(cWeek.get("me").get("rank").isNull()).isTrue();

        JsonNode prev = body(mvc.perform(get("/api/rankings").param("period", "week").param("offset", "1")
                .header("Authorization", bearer(c))));
        assertThat(prev.get("me").get("points").asInt()).isEqualTo(30);

        mvc.perform(get("/api/rankings").param("period", "day")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rankings").param("period", "year")).andExpect(status().isOk())
                .andExpect(jsonPath("$.minGames").value(30));
    }
}
