package com.binava.stafffinance.permission.controller;

import com.binava.stafffinance.permission.service.PermissionService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class PermissionController {
    private final PermissionService service;

    public PermissionController(PermissionService service) { this.service = service; }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, List<String>> permissions() { return service.permissions(); }
}
