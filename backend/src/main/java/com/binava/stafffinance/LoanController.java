package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
class LoanController {
    private final LoanService service;

    LoanController(LoanService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    List<LoanView> list(@RequestParam(required = false) Long memberId,
                        @RequestParam(required = false) LoanStatus status) {
        return service.list(memberId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    LoanView get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    LoanView create(@Valid @RequestBody LoanRequest request) { return service.create(request); }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    LoanView submit(@PathVariable Long id) { return service.submit(id); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    LoanView approve(@PathVariable Long id, @Valid @RequestBody LoanDecisionRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    LoanView reject(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.reject(id, request);
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    LoanView disburse(@PathVariable Long id, @Valid @RequestBody DisbursementRequest request) {
        return service.disburse(id, request);
    }

    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    List<ScheduleView> schedule(@PathVariable Long id) { return service.schedule(id); }

    @GetMapping("/repayments")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    List<RepaymentView> repayments(@RequestParam(required = false) Long loanId,
                                   @RequestParam(required = false) WorkflowStatus status) {
        return service.listRepayments(loanId, status);
    }

    @PostMapping("/repayments")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    RepaymentView createRepayment(@Valid @RequestBody RepaymentRequest request) {
        return service.createRepayment(request);
    }

    @PostMapping("/repayments/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    RepaymentView submitRepayment(@PathVariable Long id) {
        return service.submitRepayment(id);
    }

    @PostMapping("/repayments/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    RepaymentView decideRepayment(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideRepayment(id, request);
    }

    @GetMapping("/repayment-batches")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<BatchView> repaymentBatches() { return service.listRepaymentBatches(); }

    @PostMapping("/repayment-batches")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    BatchView createRepaymentBatch(@Valid @RequestBody RepaymentBatchRequest request) {
        return service.createRepaymentBatch(request);
    }

    @GetMapping("/repayment-batches/{id}/items")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<RepaymentView> repaymentBatchItems(@PathVariable Long id) {
        return service.repaymentBatchItems(id);
    }

    @PostMapping("/repayment-batches/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    BatchView submitRepaymentBatch(@PathVariable Long id) {
        return service.submitRepaymentBatch(id);
    }

    @PostMapping("/repayment-batches/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    BatchView decideRepaymentBatch(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideRepaymentBatch(id, request);
    }
}
