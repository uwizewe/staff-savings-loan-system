package com.binava.stafffinance.finance.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "finance_categories")
public class FinanceCategory extends BaseEntity {
    @Column(nullable = false, length = 100)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public CategoryType categoryType;

    @Column(nullable = false)
    public boolean active = true;
}
