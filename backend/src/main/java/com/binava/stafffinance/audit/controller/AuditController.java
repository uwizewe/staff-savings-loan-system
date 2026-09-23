package com.binava.stafffinance.audit.controller;

import com.binava.stafffinance.audit.dto.AuditView;
import com.binava.stafffinance.audit.service.AuditQueryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AuditController {
    private final AuditQueryService service;

    public AuditController(AuditQueryService service) { this.service = service; }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditView> auditLogs() { return service.auditLogs(); }
}
