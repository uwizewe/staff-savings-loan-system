package com.binava.stafffinance.savings.controller;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.common.dto.BatchView;
import com.binava.stafffinance.savings.dto.SavingRequest;
import com.binava.stafffinance.savings.dto.SavingView;
import com.binava.stafffinance.savings.dto.SavingsBatchRequest;
import com.binava.stafffinance.savings.entity.SavingType;
import com.binava.stafffinance.savings.service.SavingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {
    private final SavingsService service;

    public SavingsController(SavingsService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    public List<SavingView> list(@RequestParam(required = false) Long memberId,
                          @RequestParam(required = false) SavingType type,
                          @RequestParam(required = false) WorkflowStatus status) {
        return service.list(memberId, type, status);
    }

    @PostMapping("/individual")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public SavingView individual(@Valid @RequestBody SavingRequest request) {
        return service.create(request, SavingType.INDIVIDUAL);
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public SavingView withdrawal(@Valid @RequestBody SavingRequest request) {
        return service.create(request, SavingType.WITHDRAWAL);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public SavingView submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public SavingView decision(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decide(id, request);
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<BatchView> batches() {
        return service.listBatches();
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public BatchView createBatch(@Valid @RequestBody SavingsBatchRequest request) {
        return service.createBatch(request);
    }

    @GetMapping("/batches/{id}/items")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<SavingView> batchItems(@PathVariable Long id) {
        return service.batchItems(id);
    }

    @PostMapping("/batches/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public BatchView submitBatch(@PathVariable Long id) {
        return service.submitBatch(id);
    }

    @PostMapping("/batches/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public BatchView decideBatch(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideBatch(id, request);
    }
}
