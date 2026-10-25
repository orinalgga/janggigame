package com.janggi.server.auth;

import com.janggi.server.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 정지된 회원은 로그인한 채로 아무것도 할 수 없다. 자기 정보(GET /api/me)만 받아
 * 웹이 정지 사유를 보여 줄 수 있게 한다.
 */
public class BanFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public BanFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || ("GET".equals(request.getMethod()) && "/api/me".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long id = CurrentUser.id();
        if (id != null && users.existsByIdAndBannedAtIsNotNull(id)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"이용이 정지된 계정입니다\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
