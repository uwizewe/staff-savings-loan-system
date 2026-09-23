package com.binava.stafffinance.config.controller;

import com.binava.stafffinance.config.dto.SettingRequest;
import com.binava.stafffinance.config.dto.SettingView;
import com.binava.stafffinance.config.service.SystemSettingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class SystemSettingController {
    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service) { this.service = service; }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SettingView> settings() { return service.settings(); }

    @PutMapping("/settings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SettingView updateSetting(@PathVariable Long id, @Valid @RequestBody SettingRequest request) {
        return service.updateSetting(id, request);
    }
}
