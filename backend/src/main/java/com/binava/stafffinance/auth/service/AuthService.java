package com.binava.stafffinance.auth.service;

import com.binava.stafffinance.auth.dto.LoginRequest;
import com.binava.stafffinance.auth.dto.LoginResponse;
import com.binava.stafffinance.auth.entity.ApiToken;
import com.binava.stafffinance.auth.repository.TokenRepository;
import com.binava.stafffinance.auth.security.TokenHash;
import com.binava.stafffinance.exception.UnauthorizedException;
import com.binava.stafffinance.user.dto.UserView;
import com.binava.stafffinance.user.entity.AppUser;
import com.binava.stafffinance.user.mapper.UserMapper;
import com.binava.stafffinance.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final PasswordEncoder passwords;
    private final int tokenHours;

    public AuthService(UserRepository users, TokenRepository tokens, PasswordEncoder passwords,
                @Value("${app.token-hours:8}") int tokenHours) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.tokenHours = tokenHours;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!user.enabled || !passwords.matches(request.password(), user.passwordHash)) {
            throw new UnauthorizedException("Invalid username or password");
        }

        tokens.deleteAllByExpiresAtBefore(Instant.now());
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        ApiToken token = new ApiToken();
        token.tokenHash = TokenHash.hash(rawToken);
        token.user = user;
        token.expiresAt = Instant.now().plus(tokenHours, ChronoUnit.HOURS);
        tokens.save(token);
        return new LoginResponse(rawToken, UserMapper.user(user));
    }

    @Transactional(readOnly = true)
    public UserView me() {
        return UserMapper.user(currentUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHashAndExpiresAtAfter(TokenHash.hash(rawToken), Instant.now())
                .ifPresent(tokens::delete);
    }

    @Transactional(readOnly = true)
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Please sign in");
        }
        return users.findByUsernameIgnoreCase(authentication.getName())
                .filter(user -> user.enabled)
                .orElseThrow(() -> new UnauthorizedException("Your account is not available"));
    }
}
