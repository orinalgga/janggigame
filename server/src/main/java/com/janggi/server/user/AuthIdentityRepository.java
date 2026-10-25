package com.janggi.server.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

    Optional<AuthIdentity> findByProviderAndProviderUid(String provider, String providerUid);
}
