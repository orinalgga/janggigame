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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 로그인한 회원에게 주는 토큰. 회원 id만 담는다. */
@Service
public class JwtService {

    private static final Duration LIFETIME = Duration.ofDays(30);
    /** application.yml의 개발용 기본값과 같다. .env에 JANGGI_JWT_SECRET=만 있고 값이 비어도 서버가 뜨게 한다. */
    private static final String DEV_SECRET = "local-dev-secret-change-me-0123456789";

    private final SecretKey key;

    @Autowired
    public JwtService(@Value("${janggi.jwt.secret:}") String secret, @Value("${spring.datasource.url:}") String dbUrl) {
        this(secret, !dbUrl.startsWith("jdbc:h2:"));
    }

    /**
     * 개발용 키는 저장소에 공개돼 있어 누구나 아무 회원(관리자 포함)의 토큰을 만들 수 있다.
     * 그래서 운영 DB(H2가 아닌 DB)를 쓸 때는 개발용 키로 켜지지 않는다.
     */
    JwtService(String secret, boolean production) {
        boolean dev = secret == null || secret.isBlank() || secret.equals(DEV_SECRET);
        if (dev && production) {
            throw new IllegalStateException("운영 DB를 쓸 때는 JANGGI_JWT_SECRET에 32자 이상 임의 문자열을 넣어야 합니다");
        }
        byte[] bytes = (dev ? DEV_SECRET : secret).getBytes(StandardCharsets.UTF_8);
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
