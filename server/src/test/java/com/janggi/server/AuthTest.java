package com.janggi.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janggi.server.auth.AppLoginFilter;
import com.janggi.server.auth.JwtService;
import com.janggi.server.auth.OAuthLoginSuccessHandler;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthTest {

    private static final String APP = "com.janggi.app://auth";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    JwtService jwt;

    @Autowired
    ObjectMapper json;

    @Test
    void meRequiresToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsLoggedInUser() throws Exception {
        User user = users.loginWith("google", "sub-1", "kim@example.com");
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + jwt.issue(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("장기꾼" + user.getId()))
                .andExpect(jsonPath("$.nicknameChosen").value(false))
                .andExpect(jsonPath("$.rating").value(1200));
    }

    @Test
    void chooseNickname() throws Exception {
        String me = "Bearer " + jwt.issue(users.loginWith("google", "nick-1", null).getId());
        String other = "Bearer " + jwt.issue(users.loginWith("google", "nick-2", null).getId());
        mvc.perform(put("/api/me/nickname").header("Authorization", me)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\" 포장마차 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("포장마차"))
                .andExpect(jsonPath("$.nicknameChosen").value(true));
        // 내 닉네임을 다시 저장하는 건 괜찮다.
        mvc.perform(put("/api/me/nickname").header("Authorization", me)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"포장마차\"}"))
                .andExpect(status().isOk());
        for (String bad : new String[] {"포장마차", "가", "열세글자가넘는아주긴닉네임", "공 백", "장기꾼7", "운영자", "ADMIN"}) {
            mvc.perform(put("/api/me/nickname").header("Authorization", other)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + bad + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
        mvc.perform(put("/api/me/nickname").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"손님\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sameSocialAccountIsSameUser() {
        User first = users.loginWith("google", "sub-2", "lee@example.com");
        User again = users.loginWith("google", "sub-2", "lee@example.com");
        assertEquals(first.getId(), again.getId());
        // 소셜 계정의 이름이나 메일은 닉네임으로 쓰지 않는다.
        assertEquals("장기꾼" + first.getId(), first.getNickname());
    }

    @Test
    void myGamesListOnlyShowsMine() throws Exception {
        String token = jwt.issue(users.loginWith("google", "sub-3", null).getId());
        String body = mvc.perform(post("/api/games")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiSide\":\"HAN\",\"aiLevel\":\"BEGINNER\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String mine = json.readTree(body).get("id").asText();
        // 로그인하지 않고 만든 대국은 내 목록에 나오지 않는다.
        mvc.perform(post("/api/games")).andExpect(status().isCreated());

        mvc.perform(get("/api/games").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mine));
    }

    @Test
    void noProvidersWithoutKeys() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.providers.length()").value(0));
    }

    @Test
    void successHandlerRedirectsToWebWithToken() throws Exception {
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("USER")),
                Map.of("sub", "sub-4", "email", "choi@example.com", "name", "최"), "sub");
        var auth = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
        var response = new MockHttpServletResponse();

        new OAuthLoginSuccessHandler(users, jwt, "https://janggi.example", APP).onAuthenticationSuccess(
                new MockHttpServletRequest(), response, auth);

        String location = response.getRedirectedUrl();
        assertTrue(location.startsWith("https://janggi.example/#token="), location);
        long userId = jwt.verify(location.substring(location.indexOf('=') + 1)).orElseThrow();
        assertEquals("장기꾼" + userId, users.find(userId).orElseThrow().getNickname());
    }

    @Test
    void appLoginRedirectsBackToApp() throws Exception {
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("USER")),
                Map.of("sub", "sub-5", "name", "앱손님"), "sub");
        var auth = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
        // 앱이 ?app=1로 로그인을 시작하면 세션에 표시가 남는다.
        var start = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        start.setParameter("app", "1");
        new AppLoginFilter().doFilter(start, new MockHttpServletResponse(), new MockFilterChain());
        var callback = new MockHttpServletRequest();
        callback.setSession(start.getSession());
        var response = new MockHttpServletResponse();

        new OAuthLoginSuccessHandler(users, jwt, "https://janggi.example", APP).onAuthenticationSuccess(
                callback, response, auth);

        assertTrue(response.getRedirectedUrl().startsWith(APP + "#token="), response.getRedirectedUrl());
    }
}
