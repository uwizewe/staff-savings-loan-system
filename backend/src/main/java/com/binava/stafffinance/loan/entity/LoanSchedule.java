package com.binava.stafffinance.loan.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import com.binava.stafffinance.repayment.entity.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_schedules", uniqueConstraints =
        @UniqueConstraint(name = "uk_loan_installment", columnNames = {"loan_id", "installment_number"}))
public class LoanSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Loan loan;

    @Column(nullable = false)
    public int installmentNumber;
    public Integer sequenceNumber;
    public Long scheduleVersionId;
    @Column(precision=19,scale=2) public BigDecimal openingBalance;
    @Column(precision=19,scale=2) public BigDecimal interestWaived;

    @Column(nullable = false)
    public LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal expectedAmount;
    @Column(precision = 19, scale = 2) public BigDecimal principalAmount;
    @Column(precision = 19, scale = 2) public BigDecimal interestAmount;
    @Column(precision = 19, scale = 2) public BigDecimal closingBalance;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public PaymentStatus paymentStatus = PaymentStatus.PENDING;
}
