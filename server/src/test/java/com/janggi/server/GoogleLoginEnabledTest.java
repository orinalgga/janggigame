package com.janggi.server;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** 키를 넣으면 구글 로그인이 켜지고 구글 인증 화면으로 보낸다. 키는 가짜 값이다. */
@SpringBootTest(properties = {
        "janggi.oauth.google.client-id=test-client.apps.googleusercontent.com",
        "janggi.oauth.google.client-secret=test-secret"
})
@AutoConfigureMockMvc
class GoogleLoginEnabledTest {

    @Autowired
    MockMvc mvc;

    @Test
    void redirectsToGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")))
                .andExpect(header().string("Location", containsString("client_id=test-client.apps.googleusercontent.com")));
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.providers[0]").value("google"));
    }
}
