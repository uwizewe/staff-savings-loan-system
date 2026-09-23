package com.binava.stafffinance.member.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_member_code", columnList = "member_code", unique = true),
        @Index(name = "idx_member_name", columnList = "full_name")
})
public class Member extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    public String memberCode;

    @Column(nullable = false, length = 160)
    public String fullName;

    @Column(nullable = false, length = 120)
    public String department;

    @Column(nullable = false, length = 30)
    public String phone;

    @Column(nullable = false, length = 160)
    public String email;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal monthlySavingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public RiskStatus riskStatus = RiskStatus.NORMAL;

    @Column(nullable = false)
    public LocalDate joiningDate;

    public LocalDate exitDate;

    @Column(length = 1000)
    public String remarks;
}
