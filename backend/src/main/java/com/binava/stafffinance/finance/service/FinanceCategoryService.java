package com.binava.stafffinance.finance.service;

import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.finance.dto.CategoryRequest;
import com.binava.stafffinance.finance.dto.CategoryView;
import com.binava.stafffinance.finance.entity.CategoryType;
import com.binava.stafffinance.finance.entity.FinanceCategory;
import com.binava.stafffinance.finance.repository.FinanceCategoryRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceCategoryService {
    private final FinanceCategoryRepository categories;
    private final AuthService auth;
    private final AuditService audit;

    public FinanceCategoryService(FinanceCategoryRepository categories, AuthService auth, AuditService audit) {
        this.categories = categories;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories(CategoryType type, boolean activeOnly) {
        return categories.findAllByOrderByCategoryTypeAscNameAsc().stream()
                .filter(item -> type == null || item.categoryType == type)
                .filter(item -> !activeOnly || item.active)
                .map(item -> new CategoryView(item.id, item.name, item.categoryType, item.active))
                .toList();
    }

    @Transactional
    public CategoryView createCategory(CategoryRequest request) {
        FinanceCategory category = new FinanceCategory();
        category.name = request.name().trim();
        category.categoryType = request.categoryType();
        category.active = request.active() == null || request.active();
        categories.save(category);
        AppUser actor = auth.currentUser();
        audit.log(actor, "CREATE", "CATEGORY", category.id, category.name,
                null, category.categoryType, "Finance category created");
        return new CategoryView(category.id, category.name, category.categoryType, category.active);
    }

    @Transactional
    public CategoryView updateCategory(Long id, CategoryRequest request) {
        FinanceCategory category = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.name = request.name().trim();
        category.categoryType = request.categoryType();
        category.active = request.active() == null || request.active();
        AppUser actor = auth.currentUser();
        audit.log(actor, "UPDATE", "CATEGORY", category.id, category.name,
                null, category.categoryType, "Finance category updated; active=" + category.active);
        return new CategoryView(category.id, category.name, category.categoryType, category.active);
    }
}
