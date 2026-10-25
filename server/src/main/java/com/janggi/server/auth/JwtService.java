package com.janggi.server.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 로그인한 회원에게 주는 토큰. 회원 id만 담는다. */
@Service
public class JwtService {

    private static final Duration LIFETIME = Duration.ofDays(30);

    private final SecretKey key;

    public JwtService(@Value("${janggi.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("janggi.jwt.secret은 32바이트 이상이어야 합니다");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issue(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(LIFETIME)))
                .signWith(key)
                .compact();
    }

    /** 서명과 만료가 맞으면 회원 id. */
    public Optional<Long> verify(String token) {
        try {
            String sub = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(Long.parseLong(sub));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
