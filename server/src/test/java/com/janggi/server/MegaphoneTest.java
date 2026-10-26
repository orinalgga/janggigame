package com.janggi.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.server.auth.JwtService;
import com.janggi.server.shop.CoinService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "janggi.admin-emails=mega-boss@example.com")
@AutoConfigureMockMvc
class MegaphoneTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    @Autowired
    CoinService coins;

    @Autowired
    ObjectMapper json;

    @Autowired
    TransactionTemplate tx;

    private void give(User u, int coinsToGive) {
        tx.executeWithoutResult(t -> coins.change(u.getId(), coinsToGive, CoinService.Kind.ADMIN, null, "test"));
    }

    private String bearer(User u) {
        return "Bearer " + jwt.issue(u.getId());
    }

    private ResultActions send(String token, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void buyThenShoutUsesOneAndShowsToEveryone() throws Exception {
        User u = users.loginWith("google", "mega-1", null);
        String token = bearer(u);
        send(token, "/api/megaphone", "{\"text\":\"안녕하세요\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("확성기가 없어요. 먼저 사 주세요"));
        send(token, "/api/megaphone/buy", "{\"packId\":\"megaphone.1\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("엽전이 모자라요"));

        give(u, 50);
        send(token, "/api/megaphone/buy", "{\"packId\":\"megaphone.10\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10));
        mvc.perform(get("/api/shop").header("Authorization", token)).andExpect(jsonPath("$.coins").value(40));

        send(token, "/api/megaphone", "{\"text\":\"  오늘   한 판 두실 분!  \"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("오늘 한 판 두실 분!"))
                .andExpect(jsonPath("$.look.body").value("body.man"));
        mvc.perform(get("/api/megaphone").header("Authorization", token))
                .andExpect(jsonPath("$.count").value(9))
                .andExpect(jsonPath("$.recent[0].text").value("오늘 한 판 두실 분!"));
        // 누구나 볼 수 있고, 로그인하지 않으면 개수는 없다.
        mvc.perform(get("/api/megaphone")).andExpect(status().isOk()).andExpect(jsonPath("$.count").doesNotExist());
        mvc.perform(post("/api/megaphone").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isUnauthorized());

        // 바로 또 쓰면 기다리라고 하고 확성기는 그대로다.
        send(token, "/api/megaphone", "{\"text\":\"또요\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/megaphone").header("Authorization", token)).andExpect(jsonPath("$.count").value(9));
    }

    @Test
    void rudeWordsLinksAndLongTextAreRejectedWithoutSpending() throws Exception {
        User u = users.loginWith("google", "mega-2", null);
        String token = bearer(u);
        give(u, 1);
        send(token, "/api/megaphone/buy", "{\"packId\":\"megaphone.1\"}").andExpect(jsonPath("$.count").value(1));
        send(token, "/api/megaphone", "{\"text\":\"이 시 발 놈아\"}").andExpect(status().isBadRequest());
        send(token, "/api/megaphone", "{\"text\":\"싸게 팝니다 www.example.com\"}").andExpect(status().isBadRequest());
        send(token, "/api/megaphone", "{\"text\":\"" + "가".repeat(61) + "\"}").andExpect(status().isBadRequest());
        send(token, "/api/megaphone", "{\"text\":\" \"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/megaphone").header("Authorization", token)).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void adminHidesAPost() throws Exception {
        User u = users.loginWith("google", "mega-3", null);
        String token = bearer(u);
        String boss = bearer(users.loginWith("google", "mega-4", "mega-boss@example.com"));
        give(u, 1);
        send(token, "/api/megaphone/buy", "{\"packId\":\"megaphone.1\"}").andExpect(status().isOk());
        String body = send(token, "/api/megaphone", "{\"text\":\"내릴 말\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = json.readTree(body).get("id").asLong();

        mvc.perform(delete("/api/admin/megaphone/" + id).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/megaphone/" + id).header("Authorization", boss)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/megaphone/" + id).header("Authorization", boss)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/megaphone")).andExpect(jsonPath("$.recent[?(@.id==" + id + ")]").isEmpty());
        mvc.perform(get("/api/admin/logs").header("Authorization", boss))
                .andExpect(jsonPath("$[0].action").value("MEGAPHONE_HIDE"));
    }
}
