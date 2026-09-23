package com.binava.stafffinance.dashboard.controller;

import com.binava.stafffinance.dashboard.dto.DashboardView;
import com.binava.stafffinance.dashboard.service.DashboardService;

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @org.springframework.web.bind.annotation.GetMapping
    public DashboardView dashboard() { return service.dashboard(); }
}
