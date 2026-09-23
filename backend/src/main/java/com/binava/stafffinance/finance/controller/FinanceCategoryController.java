package com.binava.stafffinance.finance.controller;

import com.binava.stafffinance.finance.dto.CategoryRequest;
import com.binava.stafffinance.finance.dto.CategoryView;
import com.binava.stafffinance.finance.entity.CategoryType;
import com.binava.stafffinance.finance.service.FinanceCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class FinanceCategoryController {
    private final FinanceCategoryService service;

    public FinanceCategoryController(FinanceCategoryService service) { this.service = service; }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('INITIATOR','APPROVER','ADMIN')")
    public List<CategoryView> categories(@RequestParam(required = false) CategoryType type,
                                  @RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.categories(type, activeOnly);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }
}
