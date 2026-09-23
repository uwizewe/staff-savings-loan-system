package com.binava.stafffinance.repayment.repository;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    @Override
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    public Optional<LoanRepayment> findById(Long id);

    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    public List<LoanRepayment> findAllByOrderByPaymentDateDescCreatedAtDesc();
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    public List<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    public List<LoanRepayment> findByBatchIdOrderByLoanMemberFullNameAsc(Long batchId);
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    public List<LoanRepayment> findByWorkflowStatus(WorkflowStatus status);
}
