package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService auth;

    AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    UserView me() {
        return auth.me();
    }

    @PostMapping("/logout")
    ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        auth.logout(token);
        return ResponseEntity.ok(Map.of("message", "Signed out successfully"));
    }
}

