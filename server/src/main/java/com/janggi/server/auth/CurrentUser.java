package com.janggi.server.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 지금 요청을 보낸 회원 id. 로그인하지 않았으면 null. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Long id ? id : null;
    }
}
