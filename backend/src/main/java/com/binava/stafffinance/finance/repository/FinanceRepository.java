package com.binava.stafffinance.finance.repository;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.finance.entity.FinanceTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceRepository extends JpaRepository<FinanceTransaction, Long> {
    @EntityGraph(attributePaths = {"category", "createdBy", "actionedBy"})
    public List<FinanceTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();
    public List<FinanceTransaction> findByWorkflowStatus(WorkflowStatus status);
}
