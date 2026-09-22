package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
class FinanceController {
    private final FinanceService service;

    FinanceController(FinanceService service) { this.service = service; }

    @GetMapping
    List<FinanceView> list(@RequestParam(required = false) FinanceType type,
                           @RequestParam(required = false) WorkflowStatus status) {
        return service.list(type, status);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    FinanceView create(@Valid @RequestBody FinanceRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    FinanceView submit(@PathVariable Long id) { return service.submit(id); }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    FinanceView decision(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decide(id, request);
    }
}

