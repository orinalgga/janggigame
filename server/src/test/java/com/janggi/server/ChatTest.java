package com.janggi.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** API 키가 없는 테스트 환경이라 AI는 정해 둔 말로 답한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ChatTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String newGame(String body) throws Exception {
        String res = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(res).get("id").asText();
    }

    private ResultActions say(String id, String text) throws Exception {
        return mvc.perform(post("/api/games/" + id + "/chat").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("text", text))));
    }

    @Test
    void aiAnswersInAiGame() throws Exception {
        String id = newGame("{\"aiSide\":\"HAN\",\"aiLevel\":\"BEGINNER\"}");
        String body = say(id, "안녕하세요")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].side").value("CHO"))
                .andExpect(jsonPath("$[0].ai").value(false))
                .andExpect(jsonPath("$[1].side").value("HAN"))
                .andExpect(jsonPath("$[1].ai").value(true))
                .andExpect(jsonPath("$[1].nickname").value("새내기 장돌이"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode reply = json.readTree(body).get(1);
        assertTrue(reply.get("text").asText().startsWith("안녕하세요"));
        assertTrue(reply.get("id").asLong() > json.readTree(body).get(0).get("id").asLong());

        // AI의 첫인사, 내 말, AI의 답.
        mvc.perform(get("/api/games/" + id + "/chat"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].ai").value(true));
    }

    @Test
    void sameScreenGameHasNoChat() throws Exception {
        String id = newGame("{}");
        say(id, "안녕").andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyAndTooLongMessages() throws Exception {
        String id = newGame("{\"aiSide\":\"HAN\"}");
        say(id, "   ").andExpect(status().isBadRequest());
        say(id, "가".repeat(201)).andExpect(status().isBadRequest());
        say(id, "가".repeat(200)).andExpect(status().isOk());
    }

    @Test
    void tooFastMessagesAreRejected() throws Exception {
        String id = newGame("{\"aiSide\":\"HAN\"}");
        say(id, "하나").andExpect(status().isOk());
        say(id, "둘").andExpect(status().isBadRequest());
    }

    @Test
    void unknownGameIs404() throws Exception {
        mvc.perform(get("/api/games/00000000-0000-0000-0000-000000000000/chat")).andExpect(status().isNotFound());
    }

    @Test
    void aiGameMoveResponseCarriesOptionalComment() throws Exception {
        String id = newGame("{\"aiSide\":\"HAN\",\"aiLevel\":\"ADVANCED\"}");
        String body = mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":1,\"y\":3}}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode comment = json.readTree(body).get("aiComment");
        // 첫 수에는 잡힌 알도 장군도 없어 가끔 하는 한마디만 붙을 수 있다.
        if (!comment.isNull()) {
            assertEquals("장기 도사", comment.get("nickname").asText());
            assertEquals(2, json.readTree(mvc.perform(get("/api/games/" + id + "/chat"))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).size());
        }
    }
}
