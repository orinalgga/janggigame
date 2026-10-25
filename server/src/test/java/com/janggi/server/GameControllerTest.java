package com.janggi.server;

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

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String newGame() throws Exception {
        String body = mvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choFormation\":\"SANG_MA_SANG_MA\",\"hanFormation\":\"MA_SANG_MA_SANG\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sideToMove").value("CHO"))
                .andExpect(jsonPath("$.pieces.length()").value(32))
                .andExpect(jsonPath("$.choScore").value(72.0))
                .andExpect(jsonPath("$.result").value("ONGOING"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asText();
    }

    @Test
    void playsLegalMoveAndSwitchesTurn() throws Exception {
        String id = newGame();
        mvc.perform(post("/api/games/" + id + "/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":1,\"y\":3}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideToMove").value("HAN"));
        mvc.perform(get("/api/games/" + id)).andExpect(jsonPath("$.sideToMove").value("HAN"));
    }

    @Test
    void rejectsIllegalMove() throws Exception {
        String id = newGame();
        mvc.perform(post("/api/games/" + id + "/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":0,\"y\":2}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownGameIs404() throws Exception {
        mvc.perform(get("/api/games/nope")).andExpect(status().isNotFound());
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    }
}
