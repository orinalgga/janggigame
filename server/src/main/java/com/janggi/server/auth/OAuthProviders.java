package com.janggi.server.auth;

import java.util.List;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/** 키가 설정되어 켜진 소셜 로그인 공급자들. 비어 있으면 소셜 로그인을 끈다. */
public record OAuthProviders(List<ClientRegistration> registrations) {

    public List<String> names() {
        return registrations.stream().map(ClientRegistration::getRegistrationId).toList();
    }
}
