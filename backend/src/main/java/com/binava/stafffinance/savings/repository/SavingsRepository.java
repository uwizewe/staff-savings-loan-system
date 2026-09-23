package com.binava.stafffinance.savings.repository;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.savings.entity.SavingsTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsRepository extends JpaRepository<SavingsTransaction, Long> {
    @Override
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy", "batch"})
    public Optional<SavingsTransaction> findById(Long id);

    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy", "batch"})
    public List<SavingsTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy", "batch"})
    public List<SavingsTransaction> findByMemberIdOrderByTransactionDateDesc(Long memberId);
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy", "batch"})
    public List<SavingsTransaction> findByBatchIdOrderByMemberFullNameAsc(Long batchId);
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy", "batch"})
    public List<SavingsTransaction> findByWorkflowStatus(WorkflowStatus status);
}
