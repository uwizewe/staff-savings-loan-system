package com.binava.stafffinance.auth.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import com.binava.stafffinance.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "api_tokens", indexes = @Index(name = "idx_token_hash", columnList = "token_hash", unique = true))
public class ApiToken extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    public String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    public AppUser user;

    @Column(nullable = false)
    public Instant expiresAt;
}
