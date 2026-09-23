package com.binava.stafffinance.savings.entity;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "savings_batches")
public class SavingsBatch extends WorkflowEntity {
    @Column(nullable = false, length = 7)
    public String period;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    public String remarks;
}
