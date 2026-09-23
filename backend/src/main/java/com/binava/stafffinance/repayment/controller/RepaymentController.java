package com.binava.stafffinance.repayment.controller;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.common.dto.BatchView;
import com.binava.stafffinance.repayment.dto.RepaymentBatchRequest;
import com.binava.stafffinance.repayment.dto.RepaymentRequest;
import com.binava.stafffinance.repayment.dto.RepaymentView;
import com.binava.stafffinance.repayment.service.RepaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class RepaymentController {
    private final RepaymentService service;

    public RepaymentController(RepaymentService service) { this.service = service; }

    @GetMapping("/{id}/settlement-quote") @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public Object settlement(@PathVariable Long id) { return service.settlementQuote(id); }

    @GetMapping("/due-installments") @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public Object due(@RequestParam String period) { return service.dueInstallments(period); }

    @GetMapping("/repayments")
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public List<RepaymentView> repayments(@RequestParam(required = false) Long loanId,
                                   @RequestParam(required = false) WorkflowStatus status) {
        return service.listRepayments(loanId, status);
    }

    @GetMapping("/repayments/{id}") @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public RepaymentView repayment(@PathVariable Long id) { return service.getRepayment(id); }

    @PostMapping("/repayments")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public RepaymentView createRepayment(@Valid @RequestBody RepaymentRequest request) {
        return service.createRepayment(request);
    }

    @PostMapping("/repayments/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public RepaymentView submitRepayment(@PathVariable Long id) {
        return service.submitRepayment(id);
    }

    @PostMapping("/repayments/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public RepaymentView decideRepayment(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideRepayment(id, request);
    }

    @GetMapping("/repayment-batches")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<BatchView> repaymentBatches() { return service.listRepaymentBatches(); }

    @PostMapping("/repayment-batches")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public BatchView createRepaymentBatch(@Valid @RequestBody RepaymentBatchRequest request) {
        return service.createRepaymentBatch(request);
    }

    @GetMapping("/repayment-batches/{id}/items")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<RepaymentView> repaymentBatchItems(@PathVariable Long id) {
        return service.repaymentBatchItems(id);
    }

    @PostMapping("/repayment-batches/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public BatchView submitRepaymentBatch(@PathVariable Long id) {
        return service.submitRepaymentBatch(id);
    }

    @PostMapping("/repayment-batches/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public BatchView decideRepaymentBatch(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideRepaymentBatch(id, request);
    }
}
