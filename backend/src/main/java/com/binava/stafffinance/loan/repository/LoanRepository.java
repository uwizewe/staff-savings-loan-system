package com.binava.stafffinance.loan.repository;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.loan.entity.Loan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Override
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    public Optional<Loan> findById(Long id);

    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    public List<Loan> findAllByOrderByApplicationDateDescCreatedAtDesc();
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    public List<Loan> findByMemberIdOrderByApplicationDateDesc(Long memberId);
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    public List<Loan> findByWorkflowStatus(WorkflowStatus status);
}
