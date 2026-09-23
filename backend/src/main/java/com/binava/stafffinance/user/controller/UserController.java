package com.binava.stafffinance.user.controller;

import com.binava.stafffinance.user.dto.PasswordRequest;
import com.binava.stafffinance.user.dto.UserRequest;
import com.binava.stafffinance.user.dto.UserView;
import com.binava.stafffinance.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserView> users() { return service.users(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView createUser(@Valid @RequestBody UserRequest request) { return service.createUser(request); }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return service.updateUser(id, request);
    }

    @PostMapping("/users/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
                                                       @Valid @RequestBody PasswordRequest request) {
        service.changePassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }
}
