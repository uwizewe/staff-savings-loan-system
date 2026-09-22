package com.binava.stafffinance;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
class MemberController {
    private final MemberService service;

    MemberController(MemberService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    List<MemberView> list(@RequestParam(required = false) String q,
                          @RequestParam(required = false) MembershipStatus status,
                          @RequestParam(required = false) RiskStatus risk) {
        return service.list(q, status, risk);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    MemberView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    MemberView create(@Valid @RequestBody MemberRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATOR','ADMIN')")
    MemberView update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN','MEMBER')")
    MemberStatement statement(@PathVariable Long id) {
        return service.statement(id);
    }
}
