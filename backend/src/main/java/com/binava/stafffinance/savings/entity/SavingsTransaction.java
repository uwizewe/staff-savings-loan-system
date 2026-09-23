package com.binava.stafffinance.savings.entity;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import com.binava.stafffinance.member.entity.Member;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_transactions", indexes = {
        @Index(name = "idx_saving_member", columnList = "member_id"),
        @Index(name = "idx_saving_date", columnList = "transaction_date")
})
public class SavingsTransaction extends WorkflowEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    public SavingsBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SavingType savingType;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false)
    public LocalDate transactionDate;

    @Column(nullable = false, unique = true, length = 80)
    public String reference;

    @Column(length = 500)
    public String description;
}
