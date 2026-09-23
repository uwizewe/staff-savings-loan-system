package com.binava.stafffinance.finance.controller;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.finance.dto.FinanceRequest;
import com.binava.stafffinance.finance.dto.FinanceView;
import com.binava.stafffinance.finance.entity.FinanceType;
import com.binava.stafffinance.finance.service.FinanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) { this.service = service; }

    @GetMapping
    public List<FinanceView> list(@RequestParam(required = false) FinanceType type,
                           @RequestParam(required = false) WorkflowStatus status) {
        return service.list(type, status);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public FinanceView create(@Valid @RequestBody FinanceRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public FinanceView submit(@PathVariable Long id) { return service.submit(id); }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public FinanceView decision(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decide(id, request);
    }
}
