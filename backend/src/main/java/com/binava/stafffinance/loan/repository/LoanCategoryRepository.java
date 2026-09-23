package com.binava.stafffinance.loan.repository;
import com.binava.stafffinance.loan.entity.LoanCategory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoanCategoryRepository extends JpaRepository<LoanCategory, Long> {}
