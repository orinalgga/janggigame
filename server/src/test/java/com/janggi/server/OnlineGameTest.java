package com.janggi.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.core.Side;
import com.janggi.server.auth.JwtService;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import com.janggi.server.record.GameRecorder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OnlineGameTest {

    @LocalServerPort
    int port;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    private record Player(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Player newPlayer(String name) {
        User u = users.loginWith("google", UUID.randomUUID().toString(), null, name);
        return new Player(u.getId(), jwt.issue(u.getId()));
    }

    private JsonNode call(org.springframework.test.web.servlet.RequestBuilder request, int expected) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : json.readTree(body);
    }

    private JsonNode join(Player p) throws Exception {
        return call(post("/api/match").header("Authorization", p.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"formation\":\"SANG_MA_SANG_MA\"}"), 200);
    }

    private JsonNode move(Player p, String gameId, String body, int expected) throws Exception {
        return call(post("/api/games/" + gameId + "/moves").header("Authorization", p.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body), expected);
    }

    /** 받은 STOMP 메시지 본문(JSON)을 문자열로 쌓는다. */
    private BlockingQueue<String> subscribe(Player p, String destination) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders connect = new StompHeaders();
        connect.add("Authorization", p.bearer());
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connect, new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add(new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8));
            }
        });
        // 구독이 브로커에 등록될 시간을 준다.
        Thread.sleep(200);
        return received;
    }

    @Test
    void matchRequiresLogin() throws Exception {
        mvc.perform(post("/api/match")).andExpect(status().isUnauthorized());
    }

    @Test
    void twoPlayersAreMatchedAndWaitingPlayerIsNotified() throws Exception {
        Player a = newPlayer("가");
        Player b = newPlayer("나");
        BlockingQueue<String> aInbox = subscribe(a, "/user/queue/match");

        assertEquals("WAITING", join(a).get("status").asText());
        JsonNode matched = join(b);
        assertEquals("MATCHED", matched.get("status").asText());
        String gameId = matched.get("gameId").asText();

        String note = aInbox.poll(5, TimeUnit.SECONDS);
        assertEquals(gameId, json.readTree(note).get("gameId").asText());

        JsonNode game = call(get("/api/games/" + gameId), 200);
        assertEquals(true, game.get("online").asBoolean());
        long cho = game.get("cho").get("id").asLong();
        long han = game.get("han").get("id").asLong();
        assertNotEquals(cho, han);
        assertEquals(java.util.Set.of(a.id(), b.id()), java.util.Set.of(cho, han));
        assertEquals("CHO", game.get("clock").get("running").asText());
        assertEquals(600_000, game.get("clock").get("hanMs").asLong());
    }

    @Test
    void cancelledPlayerIsNotMatched() throws Exception {
        Player a = newPlayer("다");
        Player b = newPlayer("라");
        join(a);
        call(delete("/api/match").header("Authorization", a.bearer()), 200);
        assertEquals("WAITING", join(b).get("status").asText());
        call(delete("/api/match").header("Authorization", b.bearer()), 200);
    }

    @Test
    void onlyPlayerToMoveCanMoveAndBothSeeIt() throws Exception {
        Player a = newPlayer("마");
        Player b = newPlayer("바");
        join(a);
        String gameId = join(b).get("gameId").asText();
        JsonNode game = call(get("/api/games/" + gameId), 200);
        Player cho = game.get("cho").get("id").asLong() == a.id() ? a : b;
        Player han = cho == a ? b : a;
        BlockingQueue<String> watcher = subscribe(han, "/topic/games/" + gameId);

        String choMove = "{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":1,\"y\":3}}";
        move(han, gameId, choMove, 403);
        move(newPlayer("구경꾼"), gameId, choMove, 403);
        mvc.perform(post("/api/games/" + gameId + "/moves").contentType(MediaType.APPLICATION_JSON).content(choMove))
                .andExpect(status().isForbidden());

        JsonNode after = move(cho, gameId, choMove, 200);
        assertEquals("HAN", after.get("sideToMove").asText());
        assertEquals("HAN", after.get("clock").get("running").asText());

        JsonNode pushed = json.readTree(watcher.poll(5, TimeUnit.SECONDS));
        assertEquals("HAN", pushed.get("sideToMove").asText());
        assertEquals(1, pushed.get("lastMove").get("to").get("x").asInt());
    }

    @Test
    void resignEndsGameAndChangesRatings() throws Exception {
        Player a = newPlayer("사");
        Player b = newPlayer("아");
        join(a);
        String gameId = join(b).get("gameId").asText();
        JsonNode game = call(get("/api/games/" + gameId), 200);
        boolean aIsCho = game.get("cho").get("id").asLong() == a.id();

        JsonNode end = call(post("/api/games/" + gameId + "/resign").header("Authorization", a.bearer()), 200);
        assertEquals(aIsCho ? "HAN_WIN" : "CHO_WIN", end.get("result").asText());
        assertEquals("RESIGN", end.get("endReason").asText());
        assertEquals(0, end.get("legalMoves").size());
        // 같은 레이팅끼리면 16점이 오간다.
        assertEquals(aIsCho ? -16 : 16, end.get("ratingChange").get("cho").asInt());
        assertEquals(1184, users.find(a.id()).orElseThrow().getRating());
        assertEquals(1216, users.find(b.id()).orElseThrow().getRating());

        // 끝난 대국에는 더 둘 수 없고, 다시 기권해도 레이팅은 그대로다.
        Player winner = b;
        String anyMove = "{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":1,\"y\":3}}";
        mvc.perform(post("/api/games/" + gameId + "/moves").header("Authorization", winner.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(anyMove)).andExpect(status().isBadRequest());
        call(post("/api/games/" + gameId + "/resign").header("Authorization", a.bearer()), 200);
        assertEquals(1184, users.find(a.id()).orElseThrow().getRating());

        mvc.perform(get("/api/games").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$[0].id").value(gameId))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[0].endReason").value("RESIGN"));
    }

    @Autowired
    GameRecorder recorder;

    @Autowired
    SimpMessagingTemplate messaging;

    @Test
    void onlineGameAndClockAreRestoredAfterRestart() throws Exception {
        Player a = newPlayer("타");
        Player b = newPlayer("파");
        join(a);
        String gameId = join(b).get("gameId").asText();
        JsonNode game = call(get("/api/games/" + gameId), 200);
        Player cho = game.get("cho").get("id").asLong() == a.id() ? a : b;
        move(cho, gameId, "{\"from\":{\"x\":0,\"y\":3},\"to\":{\"x\":1,\"y\":3}}", 200);
        Thread.sleep(300);

        // 메모리가 빈 새 서비스 = 서버를 다시 켠 상황. 꺼져 있던 동안도 한의 시간이 흐른 것으로 본다.
        GameService fresh = new GameService(recorder, users, messaging, 600, 10);
        GameDto restored = fresh.get(gameId);
        assertEquals(Side.HAN, restored.clock().running());
        assertTrue(restored.clock().hanMs() <= 600_000 - 300, "한 시계: " + restored.clock().hanMs());
        assertTrue(restored.clock().choMs() > 600_000, "초는 증초를 받았다: " + restored.clock().choMs());
        assertEquals(cho.id(), restored.cho().id());
        assertEquals(true, restored.online());
    }

    @Test
    void outsiderCannotResign() throws Exception {
        Player a = newPlayer("자");
        Player b = newPlayer("차");
        join(a);
        String gameId = join(b).get("gameId").asText();
        call(post("/api/games/" + gameId + "/resign").header("Authorization", newPlayer("카").bearer()), 403);
        call(post("/api/games/" + gameId + "/resign"), 403);
    }
}
