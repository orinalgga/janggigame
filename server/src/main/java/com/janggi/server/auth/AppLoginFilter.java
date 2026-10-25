package com.janggi.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 앱이 /oauth2/authorization/google?app=1 로 로그인을 시작하면 세션에 표시해 두고,
 * 로그인이 끝났을 때 OAuthLoginSuccessHandler가 웹 대신 앱 주소로 돌려보낸다.
 */
public class AppLoginFilter extends OncePerRequestFilter {

    static final String ATTRIBUTE = "janggi.login.app";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("1".equals(request.getParameter("app"))) {
            request.getSession(true).setAttribute(ATTRIBUTE, Boolean.TRUE);
        } else if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(ATTRIBUTE);
        }
        chain.doFilter(request, response);
    }
}
