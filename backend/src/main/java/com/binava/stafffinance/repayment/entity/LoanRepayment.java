package com.binava.stafffinance.repayment.entity;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import com.binava.stafffinance.loan.entity.Loan;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_repayments", indexes = {
        @Index(name = "idx_repayment_loan", columnList = "loan_id"),
        @Index(name = "idx_repayment_date", columnList = "payment_date")
})
public class LoanRepayment extends WorkflowEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Loan loan;
    public Long scheduleId;
    public Long scheduleVersionId;
    public Integer installmentNumber;
    public String paymentType;
    @Column(precision=19,scale=2) public BigDecimal expectedInstallment;
    @Column(precision=19,scale=2) public BigDecimal principalPaid;
    @Column(precision=19,scale=2) public BigDecimal interestPaid;
    @Column(precision=19,scale=2) public BigDecimal chargesPaid;
    @Column(precision=19,scale=2) public BigDecimal outstandingBefore;
    @Column(precision=19,scale=2) public BigDecimal interestWaived;

    @ManyToOne(fetch = FetchType.LAZY)
    public RepaymentBatch batch;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false)
    public LocalDate paymentDate;

    @Column(nullable = false, unique = true, length = 80)
    public String reference;

    @Column(length = 500)
    public String remarks;
}
