package com.janggi.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.server.auth.JwtService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "janggi.admin-emails=Boss@Example.com, other@example.com")
@AutoConfigureMockMvc
class AdminTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    @Autowired
    ObjectMapper json;

    private String bearer(User u) {
        return "Bearer " + jwt.issue(u.getId());
    }

    @Test
    void onlyAdminsGetIn() throws Exception {
        String boss = bearer(users.loginWith("google", "admin-1", "boss@example.com"));
        String member = bearer(users.loginWith("google", "admin-2", "member@example.com"));
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats").header("Authorization", member)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats").header("Authorization", boss))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isNumber())
                .andExpect(jsonPath("$.revenueTotal").isNumber());
        mvc.perform(get("/api/me").header("Authorization", boss)).andExpect(jsonPath("$.admin").value(true));
        mvc.perform(get("/api/me").header("Authorization", member)).andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void searchRenameGrantAndBan() throws Exception {
        String boss = bearer(users.loginWith("google", "admin-3", "other@example.com"));
        User target = users.loginWith("google", "admin-4", "target-person@example.com");
        String targetToken = bearer(target);

        mvc.perform(get("/api/admin/users").param("q", "TARGET-person").header("Authorization", boss))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.users[0].id").value(target.getId()));
        mvc.perform(get("/api/admin/users").param("q", target.getId().toString()).header("Authorization", boss))
                .andExpect(jsonPath("$.users[0].emails[0]").value("target-person@example.com"));

        mvc.perform(put("/api/admin/users/" + target.getId() + "/nickname").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"바뀐이름\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("바뀐이름"));

        mvc.perform(post("/api/admin/users/" + target.getId() + "/items").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":\"board.night\"}"))
                .andExpect(jsonPath("$.items[0]").value("board.night"));
        mvc.perform(post("/api/admin/users/" + target.getId() + "/items").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":\"board.night\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/users/" + target.getId() + "/ban").header("Authorization", boss)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"욕설\"}"))
                .andExpect(jsonPath("$.banned").value(true));
        // 정지되면 자기 정보만 볼 수 있다.
        mvc.perform(get("/api/me").header("Authorization", targetToken))
                .andExpect(jsonPath("$.banned").value(true))
                .andExpect(jsonPath("$.banReason").value("욕설"));
        mvc.perform(post("/api/match").header("Authorization", targetToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/me/nickname").header("Authorization", targetToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"몰래바꿈\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/admin/users/" + target.getId() + "/ban").header("Authorization", boss))
                .andExpect(jsonPath("$.banned").value(false));
        mvc.perform(get("/api/shop").header("Authorization", targetToken)).andExpect(status().isOk());

        // 한 일은 최근 것부터 기록에 남는다.
        mvc.perform(get("/api/admin/logs").header("Authorization", boss))
                .andExpect(jsonPath("$[0].action").value("UNBAN"))
                .andExpect(jsonPath("$[1].action").value("BAN"))
                .andExpect(jsonPath("$[1].detail").value("욕설"))
                .andExpect(jsonPath("$[2].action").value("GRANT"))
                .andExpect(jsonPath("$[3].action").value("RENAME"))
                .andExpect(jsonPath("$[3].target").value("바뀐이름"));
        mvc.perform(get("/api/admin/logs").header("Authorization", targetToken)).andExpect(status().isForbidden());
    }

    @Test
    void adminsCannotBeBanned() throws Exception {
        User boss = users.loginWith("google", "admin-5", "boss@example.com");
        mvc.perform(post("/api/admin/users/" + boss.getId() + "/ban").header("Authorization", bearer(boss))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordersList() throws Exception {
        String boss = bearer(users.loginWith("google", "admin-6", "boss@example.com"));
        String buyer = bearer(users.loginWith("google", "admin-7", null));
        mvc.perform(post("/api/shop/orders").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"packId\":\"coins.100\"}"));
        mvc.perform(get("/api/admin/orders").header("Authorization", boss))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemTitle").value("엽전 100닢"))
                .andExpect(jsonPath("$[0].amount").value(10000))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void giveAndTakeCoins() throws Exception {
        String boss = bearer(users.loginWith("google", "admin-8", "boss@example.com"));
        User target = users.loginWith("google", "admin-9", null);
        String path = "/api/admin/users/" + target.getId() + "/coins";
        mvc.perform(post(path).header("Authorization", boss).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"memo\":\"대회 보상\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(100));
        // 가진 것보다 많이 뺄 수 없다.
        mvc.perform(post(path).header("Authorization", boss).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-150}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", boss).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-30}"))
                .andExpect(jsonPath("$.coins").value(70));
        mvc.perform(get("/api/admin/logs").header("Authorization", boss))
                .andExpect(jsonPath("$[0].action").value("COINS"))
                .andExpect(jsonPath("$[0].detail").value("-30닢 (운영자 엽전 70, 전체 70) 운영자 회수"));
        mvc.perform(get("/api/shop/coins/history").header("Authorization", bearer(target)))
                .andExpect(jsonPath("$[1].memo").value("대회 보상"))
                .andExpect(jsonPath("$[1].kind").value("ADMIN"));

        // 회원이 직접 충전한 엽전은 운영자가 뺄 수 없다.
        String member = bearer(target);
        String order = json.readTree(mvc.perform(post("/api/shop/orders").header("Authorization", member)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"packId\":\"coins.10\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).get("orderId").asText();
        mvc.perform(post("/api/shop/orders/" + order + "/confirm").header("Authorization", member)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentKey\":\"test-key\"}"))
                .andExpect(jsonPath("$.coins").value(80))
                .andExpect(jsonPath("$.freeCoins").value(70));
        mvc.perform(post(path).header("Authorization", boss).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-80}"))
                .andExpect(status().isBadRequest());

        // 살 때는 운영자 엽전부터 쓴다.
        mvc.perform(post("/api/shop/items/board.jade/buy").header("Authorization", member))
                .andExpect(jsonPath("$.coins").value(41))
                .andExpect(jsonPath("$.freeCoins").value(31));
        mvc.perform(post(path).header("Authorization", boss).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-31}"))
                .andExpect(jsonPath("$.coins").value(10))
                .andExpect(jsonPath("$.freeCoins").value(0));
        mvc.perform(get("/api/shop/coins/history").header("Authorization", member))
                .andExpect(jsonPath("$[1].kind").value("SPEND"))
                .andExpect(jsonPath("$[1].delta").value(-39))
                .andExpect(jsonPath("$[1].freeDelta").value(-39));
    }
}
