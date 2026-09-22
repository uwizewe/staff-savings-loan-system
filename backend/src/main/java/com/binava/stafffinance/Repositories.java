package com.binava.stafffinance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}

interface TokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    void deleteAllByUser(AppUser user);
    void deleteAllByExpiresAtBefore(Instant now);
}

interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberCodeIgnoreCase(String memberCode);
    boolean existsByMemberCodeIgnoreCase(String memberCode);
    List<Member> findAllByOrderByFullNameAsc();
}

interface SavingsBatchRepository extends JpaRepository<SavingsBatch, Long> {
    List<SavingsBatch> findAllByOrderByCreatedAtDesc();
}

interface SavingsRepository extends JpaRepository<SavingsTransaction, Long> {
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    List<SavingsTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();
    List<SavingsTransaction> findByMemberIdOrderByTransactionDateDesc(Long memberId);
    List<SavingsTransaction> findByBatchIdOrderByMemberFullNameAsc(Long batchId);
    List<SavingsTransaction> findByWorkflowStatus(WorkflowStatus status);
}

interface LoanRepository extends JpaRepository<Loan, Long> {
    @Override
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    Optional<Loan> findById(Long id);

    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    List<Loan> findAllByOrderByApplicationDateDescCreatedAtDesc();
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    List<Loan> findByMemberIdOrderByApplicationDateDesc(Long memberId);
    @EntityGraph(attributePaths = {"member", "createdBy", "actionedBy"})
    List<Loan> findByWorkflowStatus(WorkflowStatus status);
}

interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {
    List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
    void deleteByLoanId(Long loanId);
}

interface RepaymentBatchRepository extends JpaRepository<RepaymentBatch, Long> {
    @Override
    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    Optional<RepaymentBatch> findById(Long id);

    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    List<RepaymentBatch> findAllByOrderByCreatedAtDesc();
}

interface RepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    @Override
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    Optional<LoanRepayment> findById(Long id);

    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    List<LoanRepayment> findAllByOrderByPaymentDateDescCreatedAtDesc();
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    List<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    List<LoanRepayment> findByBatchIdOrderByLoanMemberFullNameAsc(Long batchId);
    @EntityGraph(attributePaths = {"loan.member", "createdBy", "actionedBy", "batch"})
    List<LoanRepayment> findByWorkflowStatus(WorkflowStatus status);
}

interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, Long> {
    List<FinanceCategory> findAllByOrderByCategoryTypeAscNameAsc();
    List<FinanceCategory> findByCategoryTypeAndActiveTrueOrderByNameAsc(CategoryType type);
}

interface FinanceRepository extends JpaRepository<FinanceTransaction, Long> {
    @EntityGraph(attributePaths = {"category", "createdBy", "actionedBy"})
    List<FinanceTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();
    List<FinanceTransaction> findByWorkflowStatus(WorkflowStatus status);
}

interface SettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKey(String key);
    List<SystemSetting> findAllByOrderBySettingKeyAsc();
}

interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop250ByOrderByCreatedAtDesc();
}
