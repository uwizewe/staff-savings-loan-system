package com.binava.stafffinance.loan.entity;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import com.binava.stafffinance.member.entity.Member;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_number", columnList = "application_number", unique = true),
        @Index(name = "idx_loan_member", columnList = "member_id")
})
public class Loan extends WorkflowEntity {
    @Column(unique=true) public Long committedMemberId;
    public Long categoryId;
    public java.time.LocalDate firstInstallmentDate;
    public Integer originalTerm;
    @Column(precision=19,scale=2) public BigDecimal originalPrincipal;
    @Column(precision=19,scale=2) public BigDecimal activePrincipal;
    @Column(precision=19,scale=2) public BigDecimal principalPaidBeforeSchedule;
    @Column(precision=19,scale=2) public BigDecimal interestPaidBeforeSchedule;
    public Integer installmentsPaidBeforeSchedule;
    public Long currentScheduleVersionId;
    public Long pendingScheduleVersionId;
    public String categoryName;
    public String interestMethod;
    public String adjustmentType;
    public BigDecimal adjustmentAmount;
    public Integer adjustmentMonths;
    public LocalDate adjustmentDate;
    public Long adjustmentRequestedBy;
    public String adjustmentRequestedName;
    public String adjustmentRemarks;
    @Column(nullable = false, unique = true, length = 50)
    public String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Member member;

    @Column(nullable = false)
    public LocalDate applicationDate;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal requestedAmount;

    @Column(precision = 19, scale = 2)
    public BigDecimal approvedAmount;

    @Column(nullable = false, precision = 7, scale = 3)
    public BigDecimal annualInterestRate;

    @Column(nullable = false)
    public int repaymentMonths;

    @Column(nullable = false, length = 500)
    public String purpose;

    @Column(precision = 19, scale = 2)
    public BigDecimal monthlyInstallment;

    @Column(precision = 19, scale = 2)
    public BigDecimal totalInterest;

    @Column(precision = 19, scale = 2)
    public BigDecimal totalPayable;

    @Column(precision = 19, scale = 2)
    public BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public LoanStatus loanStatus = LoanStatus.DRAFT;

    public LocalDate disbursementDate;

    @Column(length = 80)
    public String disbursementReference;

    @Column(length = 1000)
    public String remarks;
}
