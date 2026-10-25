package com.janggi.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.server.auth.JwtService;
import com.janggi.server.user.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ShopTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    @Autowired
    ObjectMapper json;

    private String login(String sub) {
        return "Bearer " + jwt.issue(users.loginWith("google", sub, null).getId());
    }

    private ResultActions send(String token, String method, String path, String body) throws Exception {
        var req = switch (method) {
            case "PUT" -> put(path);
            default -> post(path);
        };
        return mvc.perform(req.header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String orderId(String token, String itemId) throws Exception {
        String body = send(token, "POST", "/api/shop/orders", "{\"itemId\":\"" + itemId + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(3900))
                .andExpect(jsonPath("$.provider").value("test"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("orderId").asText();
    }

    @Test
    void anyoneSeesCatalogWithFreeItemsOwned() throws Exception {
        mvc.perform(get("/api/shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='board.classic')].owned").value(true))
                .andExpect(jsonPath("$[?(@.id=='board.classic')].equipped").value(true))
                .andExpect(jsonPath("$[?(@.id=='board.jade')].owned").value(false))
                .andExpect(jsonPath("$[?(@.id=='board.jade')].price").value(3900));
    }

    @Test
    void buyingNeedsLogin() throws Exception {
        mvc.perform(post("/api/shop/orders").contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":\"board.jade\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buyThenEquip() throws Exception {
        String token = login("shop-1");
        // 사기 전에는 장착할 수 없다.
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"board.jade\"}").andExpect(status().isBadRequest());

        String order = orderId(token, "board.jade");
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='board.jade')].owned").value(true));
        // 같은 주문을 다시 확인해도 괜찮고, 이미 가진 상품은 다시 주문할 수 없다.
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isOk());
        send(token, "POST", "/api/shop/orders", "{\"itemId\":\"board.jade\"}").andExpect(status().isBadRequest());

        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"board.jade\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='board.jade')].equipped").value(true))
                .andExpect(jsonPath("$[?(@.id=='board.classic')].equipped").value(false))
                .andExpect(jsonPath("$[?(@.id=='pieces.classic')].equipped").value(true));
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(jsonPath("$.boardTheme").value("board.jade"))
                .andExpect(jsonPath("$.pieceTheme").value("pieces.classic"));
    }

    @Test
    void declinedPaymentGivesNothing() throws Exception {
        String token = login("shop-2");
        String order = orderId(token, "pieces.ink");
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"fail-card\"}")
                .andExpect(status().isBadRequest());
        // 실패한 주문은 다시 확인해도 상품이 들어오지 않는다.
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/shop").header("Authorization", token))
                .andExpect(jsonPath("$[?(@.id=='pieces.ink')].owned").value(false));
    }

    @Test
    void cannotConfirmSomeoneElsesOrder() throws Exception {
        String order = orderId(login("shop-3"), "board.night");
        send(login("shop-4"), "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownItem() throws Exception {
        send(login("shop-5"), "POST", "/api/shop/orders", "{\"itemId\":\"board.nope\"}").andExpect(status().isBadRequest());
        send(login("shop-5"), "PUT", "/api/shop/equipped", "{\"itemId\":\"board.nope\"}").andExpect(status().isBadRequest());
    }
}
