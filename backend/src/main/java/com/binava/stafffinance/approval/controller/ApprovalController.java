package com.binava.stafffinance.approval.controller;

import com.binava.stafffinance.approval.dto.ApprovalView;
import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.service.ApprovalService;
import java.util.List;

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/approvals")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
public class ApprovalController {
    private final ApprovalService service;

    public ApprovalController(ApprovalService service) { this.service = service; }

    @org.springframework.web.bind.annotation.GetMapping
    public List<ApprovalView> pending() { return service.pending(); }

    @org.springframework.web.bind.annotation.PostMapping("/{type}/{id}/decision")
    public Object decide(@org.springframework.web.bind.annotation.PathVariable String type,
                  @org.springframework.web.bind.annotation.PathVariable Long id,
                  @org.springframework.web.bind.annotation.RequestBody DecisionRequest request) {
        return service.decide(type, id, request);
    }
}
