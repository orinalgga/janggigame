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

    private String orderId(String token, String packId, int amount) throws Exception {
        String body = send(token, "POST", "/api/shop/orders", "{\"packId\":\"" + packId + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.provider").value("test"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("orderId").asText();
    }

    private void charge(String token, String packId, int amount) throws Exception {
        send(token, "POST", "/api/shop/orders/" + orderId(token, packId, amount) + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isOk());
    }

    @Test
    void anyoneSeesCatalogWithFreeItemsOwned() throws Exception {
        mvc.perform(get("/api/shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").doesNotExist())
                .andExpect(jsonPath("$.items[?(@.id=='board.classic')].owned").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='board.classic')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='board.jade')].owned").value(false))
                .andExpect(jsonPath("$.items[?(@.id=='board.jade')].price").value(39))
                .andExpect(jsonPath("$.packs[?(@.id=='coins.50')].price").value(5000));
    }

    @Test
    void buyingNeedsLogin() throws Exception {
        mvc.perform(post("/api/shop/orders").contentType(MediaType.APPLICATION_JSON).content("{\"packId\":\"coins.10\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/shop/items/board.jade/buy")).andExpect(status().isUnauthorized());
    }

    @Test
    void chargeCoinsThenBuyAndEquip() throws Exception {
        String token = login("shop-1");
        mvc.perform(get("/api/shop").header("Authorization", token)).andExpect(jsonPath("$.coins").value(0));
        // 엽전이 없으면 살 수 없고, 사기 전에는 장착할 수 없다.
        send(token, "POST", "/api/shop/items/board.jade/buy", "").andExpect(status().isBadRequest());
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"board.jade\"}").andExpect(status().isBadRequest());

        String order = orderId(token, "coins.50", 5000);
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(50));
        // 같은 주문을 다시 확인해도 한 번만 들어간다.
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(jsonPath("$.coins").value(50));

        send(token, "POST", "/api/shop/items/board.jade/buy", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(11))
                .andExpect(jsonPath("$.items[?(@.id=='board.jade')].owned").value(true));
        // 이미 가진 상품, 엽전이 모자란 상품은 살 수 없다.
        send(token, "POST", "/api/shop/items/board.jade/buy", "").andExpect(status().isBadRequest());
        send(token, "POST", "/api/shop/items/pieces.gold/buy", "").andExpect(status().isBadRequest());

        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"board.jade\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='board.jade')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='board.classic')].equipped").value(false))
                .andExpect(jsonPath("$.items[?(@.id=='pieces.classic')].equipped").value(true));
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(jsonPath("$.boardTheme").value("board.jade"))
                .andExpect(jsonPath("$.pieceTheme").value("pieces.classic"));
        mvc.perform(get("/api/shop/coins/history").header("Authorization", token))
                .andExpect(jsonPath("$[0].kind").value("SPEND"))
                .andExpect(jsonPath("$[0].delta").value(-39))
                .andExpect(jsonPath("$[0].balance").value(11))
                .andExpect(jsonPath("$[1].kind").value("CHARGE"))
                .andExpect(jsonPath("$[1].delta").value(50));
    }

    @Test
    void avatarsAreBoughtWithCoinsButRewardsAreNotForSale() throws Exception {
        String token = login("shop-avatar");
        charge(token, "coins.50", 5000);
        send(token, "POST", "/api/shop/items/avatar.pine/buy", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(50 - 20))
                .andExpect(jsonPath("$.items[?(@.id=='avatar.basic')].equipped").value(true));
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"avatar.pine\"}")
                .andExpect(jsonPath("$.items[?(@.id=='avatar.pine')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='avatar.basic')].equipped").value(false))
                .andExpect(jsonPath("$.items[?(@.id=='board.classic')].equipped").value(true));
        mvc.perform(get("/api/me").header("Authorization", token)).andExpect(jsonPath("$.look.background").value("avatar.pine"));
        // 대회 보상은 엽전으로 살 수 없고, 얻기 전에는 장착할 수도 없다.
        send(token, "POST", "/api/shop/items/avatar.crown/buy", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("대회 우승으로만 얻을 수 있는 상품입니다"));
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"avatar.crown\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/shop").header("Authorization", token))
                .andExpect(jsonPath("$.coins").value(50 - 20))
                .andExpect(jsonPath("$.items[?(@.id=='avatar.crown')].reward").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='avatar.crown')].owned").value(false));
    }

    @Test
    void characterPartsAreWornTogether() throws Exception {
        String token = login("shop-character");
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(jsonPath("$.look.style").value("style.doll"))
                .andExpect(jsonPath("$.look.body").value("body.man"))
                .andExpect(jsonPath("$.look.hair").value("hair.short"))
                .andExpect(jsonPath("$.look.beard").value("beard.none"))
                .andExpect(jsonPath("$.look.outfit").value("outfit.tee"))
                .andExpect(jsonPath("$.look.hat").value("hat.none"));
        // 무료 부품(몸, 기본 머리)은 사지 않고 바로 입는다. 유료 부품은 사야 한다.
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"body.woman\"}").andExpect(status().isOk());
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"hair.jjok\"}")
                .andExpect(jsonPath("$.items[?(@.id=='hair.jjok')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='hair.short')].equipped").value(false));
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"outfit.dangui\"}").andExpect(status().isBadRequest());
        charge(token, "coins.100", 10000);
        send(token, "POST", "/api/shop/items/outfit.dangui/buy", "")
                .andExpect(jsonPath("$.coins").value(100 - 30));
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"outfit.dangui\"}")
                .andExpect(jsonPath("$.items[?(@.id=='outfit.dangui')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='body.woman')].equipped").value(true));
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(jsonPath("$.look.body").value("body.woman"))
                .andExpect(jsonPath("$.look.hair").value("hair.jjok"))
                .andExpect(jsonPath("$.look.outfit").value("outfit.dangui"))
                .andExpect(jsonPath("$.look.background").value("avatar.basic"));
        // 화풍도 사서 갈아입는다.
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"style.anime\"}").andExpect(status().isBadRequest());
        send(token, "POST", "/api/shop/items/style.anime/buy", "").andExpect(jsonPath("$.coins").value(100 - 30 - 25));
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"style.anime\"}")
                .andExpect(jsonPath("$.items[?(@.id=='style.anime')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='style.doll')].equipped").value(false));
        mvc.perform(get("/api/me").header("Authorization", token)).andExpect(jsonPath("$.look.style").value("style.anime"));
    }

    @Test
    void bundleChargesOnlyMissingPartsAtADiscountAndWearsThem() throws Exception {
        String token = login("shop-bundle");
        mvc.perform(get("/api/shop").header("Authorization", token))
                .andExpect(jsonPath("$.bundles[?(@.id=='set.seonbi')].fullPrice").value(20 + 15 + 15 + 20))
                .andExpect(jsonPath("$.bundles[?(@.id=='set.seonbi')].price").value((20 + 15 + 15 + 20) * 80 / 100));
        charge(token, "coins.300", 30000);
        // 흑립을 먼저 사 두면 세트 값에서 빠진다.
        send(token, "POST", "/api/shop/items/hat.gat/buy", "").andExpect(jsonPath("$.coins").value(300 - 15));
        send(token, "POST", "/api/shop/bundles/set.seonbi/buy", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(300 - 15 - (20 + 15 + 20) * 80 / 100))
                .andExpect(jsonPath("$.bundles[?(@.id=='set.seonbi')].owned").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='outfit.dopo')].equipped").value(true))
                .andExpect(jsonPath("$.items[?(@.id=='avatar.pine')].equipped").value(true));
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(jsonPath("$.look.beard").value("beard.scholar"))
                .andExpect(jsonPath("$.look.hat").value("hat.gat"));
        send(token, "POST", "/api/shop/bundles/set.seonbi/buy", "").andExpect(status().isBadRequest());
        send(token, "POST", "/api/shop/bundles/set.nope/buy", "").andExpect(status().isBadRequest());
    }

    @Test
    void declinedPaymentGivesNothing() throws Exception {
        String token = login("shop-2");
        String order = orderId(token, "coins.10", 1000);
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"fail-card\"}")
                .andExpect(status().isBadRequest());
        // 실패한 주문은 다시 확인해도 엽전이 들어오지 않는다.
        send(token, "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/shop").header("Authorization", token)).andExpect(jsonPath("$.coins").value(0));
    }

    @Test
    void cannotConfirmSomeoneElsesOrder() throws Exception {
        String order = orderId(login("shop-3"), "coins.30", 3000);
        send(login("shop-4"), "POST", "/api/shop/orders/" + order + "/confirm", "{\"paymentKey\":\"test-key\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownItem() throws Exception {
        String token = login("shop-5");
        charge(token, "coins.10", 1000);
        send(token, "POST", "/api/shop/orders", "{\"packId\":\"coins.7\"}").andExpect(status().isBadRequest());
        send(token, "POST", "/api/shop/items/board.nope/buy", "").andExpect(status().isBadRequest());
        send(token, "PUT", "/api/shop/equipped", "{\"itemId\":\"board.nope\"}").andExpect(status().isBadRequest());
    }
}
