package com.janggi.server.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 소셜 계정 하나. provider는 google, kakao, apple 같은 이름. */
@Entity
@Table(name = "auth_identities")
public class AuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_uid", nullable = false)
    private String providerUid;

    private String email;

    protected AuthIdentity() {
    }

    public AuthIdentity(User user, String provider, String providerUid, String email) {
        this.user = user;
        this.provider = provider;
        this.providerUid = providerUid;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public User getUser() {
        return user;
    }
}
