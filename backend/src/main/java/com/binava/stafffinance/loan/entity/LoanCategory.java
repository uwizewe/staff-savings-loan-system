package com.binava.stafffinance.loan.entity;
import com.binava.stafffinance.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="loan_categories")
public class LoanCategory extends BaseEntity {
 @Column(nullable=false, unique=true, length=100) public String name;
 @Column(length=1000) public String description;
 @Column(nullable=false, columnDefinition="boolean default true") public boolean active=true;
 public String createdBy;
 public String modifiedBy;
 @Column(nullable=false, precision=7, scale=3) public BigDecimal annualRate;
}
