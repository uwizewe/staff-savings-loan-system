package com.binava.stafffinance.finance.repository;

import com.binava.stafffinance.finance.entity.CategoryType;
import com.binava.stafffinance.finance.entity.FinanceCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, Long> {
    public List<FinanceCategory> findAllByOrderByCategoryTypeAscNameAsc();
    public List<FinanceCategory> findByCategoryTypeAndActiveTrueOrderByNameAsc(CategoryType type);
}
