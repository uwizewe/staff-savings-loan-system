package com.binava.stafffinance.member.controller;

import com.binava.stafffinance.member.dto.MemberRequest;
import com.binava.stafffinance.member.dto.MemberStatement;
import com.binava.stafffinance.member.dto.MemberView;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.entity.RiskStatus;
import com.binava.stafffinance.member.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService service;

    public MemberController(MemberService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<MemberView> list(@RequestParam(required = false) String q,
                          @RequestParam(required = false) MembershipStatus status,
                          @RequestParam(required = false) RiskStatus risk) {
        return service.list(q, status, risk);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public MemberView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public MemberView create(@Valid @RequestBody MemberRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    public MemberView update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN','MEMBER')")
    public MemberStatement statement(@PathVariable Long id) {
        return service.statement(id);
    }
}
