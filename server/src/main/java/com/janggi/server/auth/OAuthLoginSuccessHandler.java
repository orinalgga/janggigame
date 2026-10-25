package com.janggi.server.auth;

import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 소셜 로그인이 끝나면 회원을 찾거나 만들고, 웹으로 돌려보내며 토큰을 URL 조각(#token=)에 실어 준다.
 * 조각은 서버로 전송되지 않고 웹 주소와 API 주소가 달라도 쓸 수 있다.
 */
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService users;
    private final JwtService jwt;
    private final String webBaseUrl;

    public OAuthLoginSuccessHandler(UserService users, JwtService jwt, String webBaseUrl) {
        this.users = users;
        this.jwt = jwt;
        this.webBaseUrl = webBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        OAuth2User principal = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        User user = users.loginWith(provider, principal.getName(),
                principal.getAttribute("email"), principal.getAttribute("name"));
        // 로그인 과정에서만 쓴 세션은 더 필요 없다.
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(webBaseUrl + "/#token=" + jwt.issue(user.getId()));
    }
}
