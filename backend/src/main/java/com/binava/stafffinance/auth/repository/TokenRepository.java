package com.binava.stafffinance.auth.repository;

import com.binava.stafffinance.auth.entity.ApiToken;
import com.binava.stafffinance.user.entity.AppUser;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<ApiToken, Long> {
    public Optional<ApiToken> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    public void deleteAllByUser(AppUser user);
    public void deleteAllByExpiresAtBefore(Instant now);
}
