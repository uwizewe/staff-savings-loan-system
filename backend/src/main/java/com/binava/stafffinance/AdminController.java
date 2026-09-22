package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
class AdminController {
    private final AdminService service;

    AdminController(AdminService service) { this.service = service; }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    List<UserView> users() { return service.users(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    UserView createUser(@Valid @RequestBody UserRequest request) { return service.createUser(request); }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UserView updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return service.updateUser(id, request);
    }

    @PostMapping("/users/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
                                                       @Valid @RequestBody PasswordRequest request) {
        service.changePassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    Map<String, List<String>> permissions() { return service.permissions(); }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<CategoryView> categories(@RequestParam(required = false) CategoryType type,
                                  @RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.categories(type, activeOnly);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    List<SettingView> settings() { return service.settings(); }

    @PutMapping("/settings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    SettingView updateSetting(@PathVariable Long id, @Valid @RequestBody SettingRequest request) {
        return service.updateSetting(id, request);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    List<AuditView> auditLogs() { return service.auditLogs(); }
}

