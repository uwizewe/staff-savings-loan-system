package com.binava.stafffinance.repayment.entity;
import com.binava.stafffinance.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="repayment_allocations")
public class RepaymentAllocation extends BaseEntity {
    public Long repaymentId;
    public Long scheduleId;
    public Long scheduleVersionId;
    public int installmentNumber;
    @Column(precision=19,scale=2) public BigDecimal principal;
    @Column(precision=19,scale=2) public BigDecimal interest;
    @Column(precision=19,scale=2) public BigDecimal interestWaived;
}
