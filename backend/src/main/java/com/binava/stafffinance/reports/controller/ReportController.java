package com.binava.stafffinance.reports.controller;

import com.binava.stafffinance.reports.service.ReportService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports=reports; }

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public Map<String,Object> snapshot() { return reports.snapshot(); }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> audit() { return reports.audit(); }
}
