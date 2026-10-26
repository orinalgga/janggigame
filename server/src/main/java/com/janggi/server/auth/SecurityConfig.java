package com.janggi.server.auth;

import com.janggi.server.user.UserRepository;
import com.janggi.server.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API는 토큰(JWT)으로 인증하고 세션을 쓰지 않는다. 소셜 로그인은 키가 설정된 공급자만 켠다.
 * 키가 하나도 없으면 로그인 없이도 서버가 그대로 돈다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserService users, UserRepository userRepository,
            OAuthProviders providers, @Value("${janggi.web-base-url}") String webBaseUrl,
            @Value("${janggi.app-redirect-url}") String appRedirectUrl)
            throws Exception {
        http.csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                // 소셜 로그인 왕복 동안에만 세션이 생긴다.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a -> a
                        // 상품 목록은 누구나 본다. 사기와 장착은 로그인해야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/shop").permitAll()
                        // 대회는 누구나 보고, 참가·기권은 로그인해야 한다.
                        .requestMatchers(HttpMethod.POST, "/api/tournaments", "/api/tournaments/**").authenticated()
                        .requestMatchers("/api/me", "/api/me/**", "/api/match", "/api/shop/**", "/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new BanFilter(userRepository), JwtAuthFilter.class)
                .addFilterBefore(new AppLoginFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
        if (!providers.registrations().isEmpty()) {
            http.oauth2Login(o -> o
                    .clientRegistrationRepository(new InMemoryClientRegistrationRepository(providers.registrations()))
                    .successHandler(new OAuthLoginSuccessHandler(users, jwt, webBaseUrl, appRedirectUrl)));
        }
        return http.build();
    }

    @Bean
    OAuthProviders oauthProviders(
            @Value("${janggi.oauth.google.client-id:}") String googleId,
            @Value("${janggi.oauth.google.client-secret:}") String googleSecret) {
        List<ClientRegistration> list = new ArrayList<>();
        if (!googleId.isBlank() && !googleSecret.isBlank()) {
            list.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleId)
                    .clientSecret(googleSecret)
                    .scope("openid", "profile", "email")
                    .build());
        }
        return new OAuthProviders(list);
    }
}
