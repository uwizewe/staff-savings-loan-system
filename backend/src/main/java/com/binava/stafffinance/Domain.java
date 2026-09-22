package com.binava.stafffinance;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

enum Role { MEMBER, INITIATOR, APPROVER, ADMIN }
enum MembershipStatus { ACTIVE, DISABLED, LEFT }
enum RiskStatus { NORMAL, WATCHLIST }
enum WorkflowStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, REVERSED }
enum SavingType { MONTHLY, INDIVIDUAL, WITHDRAWAL, ADJUSTMENT }
enum LoanStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, ACTIVE, COMPLETED }
enum PaymentStatus { PENDING, PARTIAL, PAID, OVERDUE }
enum FinanceType { INCOME, EXPENSE }
enum CategoryType { INCOME, EXPENSE }

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    Instant updatedAt;

    @Version
    Long version;
}

@MappedSuperclass
abstract class WorkflowEntity extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    WorkflowStatus workflowStatus = WorkflowStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    AppUser submittedBy;

    Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    AppUser actionedBy;

    Instant actionedAt;

    @Column(length = 500)
    String decisionRemarks;
}

@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_member_code", columnList = "member_code", unique = true),
        @Index(name = "idx_member_name", columnList = "full_name")
})
class Member extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    String memberCode;

    @Column(nullable = false, length = 160)
    String fullName;

    @Column(nullable = false, length = 120)
    String department;

    @Column(nullable = false, length = 30)
    String phone;

    @Column(nullable = false, length = 160)
    String email;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal monthlySavingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RiskStatus riskStatus = RiskStatus.NORMAL;

    @Column(nullable = false)
    LocalDate joiningDate;

    LocalDate exitDate;

    @Column(length = 1000)
    String remarks;
}

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_user_username", columnList = "username", unique = true))
class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    String username;

    @Column(nullable = false)
    String passwordHash;

    @Column(nullable = false, length = 160)
    String fullName;

    @Column(nullable = false, length = 160)
    String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Role role;

    @Column(nullable = false)
    boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    Member member;
}

@Entity
@Table(name = "api_tokens", indexes = @Index(name = "idx_token_hash", columnList = "token_hash", unique = true))
class ApiToken extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    AppUser user;

    @Column(nullable = false)
    Instant expiresAt;
}

@Entity
@Table(name = "savings_batches")
class SavingsBatch extends WorkflowEntity {
    @Column(nullable = false, length = 7)
    String period;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    String remarks;
}

@Entity
@Table(name = "savings_transactions", indexes = {
        @Index(name = "idx_saving_member", columnList = "member_id"),
        @Index(name = "idx_saving_date", columnList = "transaction_date")
})
class SavingsTransaction extends WorkflowEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    SavingsBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SavingType savingType;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(nullable = false)
    LocalDate transactionDate;

    @Column(nullable = false, unique = true, length = 80)
    String reference;

    @Column(length = 500)
    String description;
}

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_number", columnList = "application_number", unique = true),
        @Index(name = "idx_loan_member", columnList = "member_id")
})
class Loan extends WorkflowEntity {
    @Column(nullable = false, unique = true, length = 50)
    String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Member member;

    @Column(nullable = false)
    LocalDate applicationDate;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal requestedAmount;

    @Column(precision = 19, scale = 2)
    BigDecimal approvedAmount;

    @Column(nullable = false, precision = 7, scale = 3)
    BigDecimal annualInterestRate;

    @Column(nullable = false)
    int repaymentMonths;

    @Column(nullable = false, length = 500)
    String purpose;

    @Column(precision = 19, scale = 2)
    BigDecimal monthlyInstallment;

    @Column(precision = 19, scale = 2)
    BigDecimal totalInterest;

    @Column(precision = 19, scale = 2)
    BigDecimal totalPayable;

    @Column(precision = 19, scale = 2)
    BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    LoanStatus loanStatus = LoanStatus.DRAFT;

    LocalDate disbursementDate;

    @Column(length = 80)
    String disbursementReference;

    @Column(length = 1000)
    String remarks;
}

@Entity
@Table(name = "loan_schedules", uniqueConstraints =
        @UniqueConstraint(name = "uk_loan_installment", columnNames = {"loan_id", "installment_number"}))
class LoanSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Loan loan;

    @Column(nullable = false)
    int installmentNumber;

    @Column(nullable = false)
    LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal expectedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentStatus paymentStatus = PaymentStatus.PENDING;
}

@Entity
@Table(name = "repayment_batches")
class RepaymentBatch extends WorkflowEntity {
    @Column(nullable = false, length = 7)
    String period;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    String remarks;
}

@Entity
@Table(name = "loan_repayments", indexes = {
        @Index(name = "idx_repayment_loan", columnList = "loan_id"),
        @Index(name = "idx_repayment_date", columnList = "payment_date")
})
class LoanRepayment extends WorkflowEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    RepaymentBatch batch;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(nullable = false)
    LocalDate paymentDate;

    @Column(nullable = false, unique = true, length = 80)
    String reference;

    @Column(length = 500)
    String remarks;
}

@Entity
@Table(name = "finance_categories")
class FinanceCategory extends BaseEntity {
    @Column(nullable = false, length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CategoryType categoryType;

    @Column(nullable = false)
    boolean active = true;
}

@Entity
@Table(name = "finance_transactions", indexes = @Index(name = "idx_finance_date", columnList = "transaction_date"))
class FinanceTransaction extends WorkflowEntity {
    @Column(nullable = false, unique = true, length = 80)
    String reference;

    @Column(nullable = false)
    LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    FinanceType financeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    FinanceCategory category;

    @Column(nullable = false, length = 500)
    String description;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(length = 250)
    String supportingReference;
}

@Entity
@Table(name = "system_settings")
class SystemSetting extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    String settingKey;

    @Column(nullable = false, length = 500)
    String settingValue;

    @Column(nullable = false, length = 250)
    String description;
}

@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_time", columnList = "created_at"))
class AuditLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    AppUser user;

    @Column(nullable = false, length = 80)
    String action;

    @Column(nullable = false, length = 80)
    String entityType;

    Long entityId;

    @Column(length = 100)
    String reference;

    @Column(length = 30)
    String previousStatus;

    @Column(length = 30)
    String newStatus;

    @Column(length = 1000)
    String details;
}
