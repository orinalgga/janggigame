package com.janggi.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.janggi.server.auth.JwtService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
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
                .contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":\"pieces.gold\"}"));
        mvc.perform(get("/api/admin/orders").header("Authorization", boss))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemTitle").value("금박 알"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
