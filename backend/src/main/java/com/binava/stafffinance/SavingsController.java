package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings")
class SavingsController {
    private final SavingsService service;

    SavingsController(SavingsService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','INITIATOR','APPROVER','ADMIN')")
    List<SavingView> list(@RequestParam(required = false) Long memberId,
                          @RequestParam(required = false) SavingType type,
                          @RequestParam(required = false) WorkflowStatus status) {
        return service.list(memberId, type, status);
    }

    @PostMapping("/individual")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    SavingView individual(@Valid @RequestBody SavingRequest request) {
        return service.create(request, SavingType.INDIVIDUAL);
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    SavingView withdrawal(@Valid @RequestBody SavingRequest request) {
        return service.create(request, SavingType.WITHDRAWAL);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    SavingView submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    SavingView decision(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decide(id, request);
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<BatchView> batches() {
        return service.listBatches();
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    BatchView createBatch(@Valid @RequestBody SavingsBatchRequest request) {
        return service.createBatch(request);
    }

    @GetMapping("/batches/{id}/items")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<SavingView> batchItems(@PathVariable Long id) {
        return service.batchItems(id);
    }

    @PostMapping("/batches/{id}/submit")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    BatchView submitBatch(@PathVariable Long id) {
        return service.submitBatch(id);
    }

    @PostMapping("/batches/{id}/decision")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    BatchView decideBatch(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return service.decideBatch(id, request);
    }
}

