package com.binava.stafffinance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
class AuthService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final PasswordEncoder passwords;
    private final int tokenHours;

    AuthService(UserRepository users, TokenRepository tokens, PasswordEncoder passwords,
                @Value("${app.token-hours:8}") int tokenHours) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.tokenHours = tokenHours;
    }

    @Transactional
    LoginResponse login(LoginRequest request) {
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
        return new LoginResponse(rawToken, Views.user(user));
    }

    @Transactional(readOnly = true)
    UserView me() {
        return Views.user(currentUser());
    }

    @Transactional
    void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHashAndExpiresAtAfter(TokenHash.hash(rawToken), Instant.now())
                .ifPresent(tokens::delete);
    }

    @Transactional(readOnly = true)
    AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Please sign in");
        }
        return users.findByUsernameIgnoreCase(authentication.getName())
                .filter(user -> user.enabled)
                .orElseThrow(() -> new UnauthorizedException("Your account is not available"));
    }
}

