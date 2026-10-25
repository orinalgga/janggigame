package com.janggi.server.user;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int MAX_NICKNAME = 40;

    private final UserRepository users;
    private final AuthIdentityRepository identities;

    public UserService(UserRepository users, AuthIdentityRepository identities) {
        this.users = users;
        this.identities = identities;
    }

    /** 처음 들어온 소셜 계정이면 회원을 새로 만들고, 아니면 연결된 회원을 돌려준다. */
    @Transactional
    public User loginWith(String provider, String providerUid, String email, String name) {
        return identities.findByProviderAndProviderUid(provider, providerUid)
                .map(AuthIdentity::getUser)
                .orElseGet(() -> {
                    User user = users.save(new User(nickname(name, email), OffsetDateTime.now()));
                    identities.save(new AuthIdentity(user, provider, providerUid, email));
                    return user;
                });
    }

    @Transactional(readOnly = true)
    public Optional<User> find(long id) {
        return users.findById(id);
    }

    private static String nickname(String name, String email) {
        String n = name != null && !name.isBlank() ? name.strip()
                : email != null && email.contains("@") ? email.substring(0, email.indexOf('@'))
                : "장기꾼";
        return n.length() > MAX_NICKNAME ? n.substring(0, MAX_NICKNAME) : n;
    }
}
