package com.binava.stafffinance.loan.controller;

import com.binava.stafffinance.approval.dto.DecisionRequest;

import com.binava.stafffinance.loan.dto.LoanDecisionRequest;
import com.binava.stafffinance.loan.dto.LoanRequest;
import com.binava.stafffinance.loan.dto.LoanView;
import com.binava.stafffinance.loan.dto.ScheduleView;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public List<LoanView> list(@RequestParam(required = false) Long memberId,
                        @RequestParam(required = false) LoanStatus status) {
        return service.list(memberId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public LoanView get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public LoanView create(@Valid @RequestBody LoanRequest request, @RequestParam(required=false) Long categoryId) { return categoryId == null ? service.create(request) : service.createCategorized(request, categoryId); }
    @PostMapping("/preview") @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public com.binava.stafffinance.loan.service.LoanCalculationService.Calculation preview(@Valid @RequestBody LoanRequest request, @RequestParam(required=false) Long categoryId) {
        return service.preview(request,categoryId);
    }
    @PostMapping("/{id}/adjustment-preview") @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public Object previewAdjustment(@PathVariable Long id,@Valid @RequestBody com.binava.stafffinance.loan.dto.AdjustmentRequest request) { return service.adjustmentPreview(id,request); }
    @PostMapping("/{id}/adjustments") @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public LoanView adjust(@PathVariable Long id,@Valid @RequestBody com.binava.stafffinance.loan.dto.AdjustmentRequest request) { return service.requestAdjustment(id,request); }
    @PostMapping("/{id}/adjustments/decision") @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public LoanView decideAdjustment(@PathVariable Long id,@RequestBody DecisionRequest request) { return service.decideAdjustment(id,request); }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public LoanView submit(@PathVariable Long id) { return service.submit(id); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public LoanView approve(@PathVariable Long id, @Valid @RequestBody LoanDecisionRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public LoanView reject(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.reject(id, request);
    }

    @GetMapping("/{id}/schedule-history")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public Object history(@PathVariable Long id) { return service.scheduleHistory(id); }

    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public List<ScheduleView> schedule(@PathVariable Long id) { return service.schedule(id); }

}
