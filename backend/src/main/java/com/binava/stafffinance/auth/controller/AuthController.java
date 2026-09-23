package com.binava.stafffinance.auth.controller;

import com.binava.stafffinance.auth.dto.LoginRequest;
import com.binava.stafffinance.auth.dto.LoginResponse;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.user.dto.UserView;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public UserView me() {
        return auth.me();
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        auth.logout(token);
        return ResponseEntity.ok(Map.of("message", "Signed out successfully"));
    }
}
