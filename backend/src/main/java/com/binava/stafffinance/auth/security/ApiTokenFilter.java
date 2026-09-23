package com.binava.stafffinance.auth.security;

import com.binava.stafffinance.auth.repository.TokenRepository;
import com.binava.stafffinance.user.entity.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiTokenFilter extends OncePerRequestFilter {
    private final TokenRepository tokens;

    public ApiTokenFilter(TokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") &&
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawToken = header.substring(7).trim();
            tokens.findByTokenHashAndExpiresAtAfter(TokenHash.hash(rawToken), Instant.now())
                    .filter(token -> token.user.enabled)
                    .ifPresent(token -> {
                        AppUser user = token.user;
                        var authority = new SimpleGrantedAuthority("ROLE_" + user.role.name());
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.username, null, List.of(authority));
                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
