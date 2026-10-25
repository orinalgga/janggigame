package com.janggi.server.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void blankSecretFallsBackToDevSecret() {
        // .env의 빈 JANGGI_JWT_SECRET= 때문에 서버가 뜨지 않던 문제.
        JwtService jwt = new JwtService("", false);
        assertEquals(42L, jwt.verify(jwt.issue(42)).orElseThrow());
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", false));
    }

    @Test
    void productionRefusesDevSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", true));
        assertThrows(IllegalStateException.class, () -> new JwtService("local-dev-secret-change-me-0123456789", true));
        JwtService jwt = new JwtService("a-real-secret-that-is-long-enough-1234567", true);
        assertEquals(7L, jwt.verify(jwt.issue(7)).orElseThrow());
    }
}
