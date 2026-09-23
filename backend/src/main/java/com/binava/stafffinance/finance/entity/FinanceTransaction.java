package com.binava.stafffinance.finance.entity;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "finance_transactions", indexes = @Index(name = "idx_finance_date", columnList = "transaction_date"))
public class FinanceTransaction extends WorkflowEntity {
    @Column(nullable = false, unique = true, length = 80)
    public String reference;

    @Column(nullable = false)
    public LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public FinanceType financeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public FinanceCategory category;

    @Column(nullable = false, length = 500)
    public String description;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(length = 250)
    public String supportingReference;
}
