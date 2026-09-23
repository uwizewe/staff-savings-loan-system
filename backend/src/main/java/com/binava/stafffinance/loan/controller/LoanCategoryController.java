package com.binava.stafffinance.loan.controller;
import com.binava.stafffinance.loan.service.LoanCategoryService;
import com.binava.stafffinance.loan.entity.LoanCategory;
import com.binava.stafffinance.loan.dto.CategoryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/loan-categories")
public class LoanCategoryController {
 private final LoanCategoryService service;
 public LoanCategoryController(LoanCategoryService service) { this.service=service; }
 @GetMapping @PreAuthorize("isAuthenticated()") public List<LoanCategory> list(@RequestParam(defaultValue="false") boolean activeOnly) { return service.list(activeOnly); }
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public LoanCategory create(@Valid @RequestBody CategoryRequest request) { return service.save(null,request); }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public LoanCategory update(@PathVariable Long id,@Valid @RequestBody CategoryRequest request) { return service.save(id,request); }
}
