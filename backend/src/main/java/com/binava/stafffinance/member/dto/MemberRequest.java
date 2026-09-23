package com.binava.stafffinance.member.dto;

import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.entity.RiskStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberRequest(
        String memberCode,
        @NotBlank String fullName,
        @NotBlank String department,
        @NotBlank String phone,
        @Email @NotBlank String email,
        @NotNull @DecimalMin("0.00") BigDecimal monthlySavingAmount,
        @NotNull MembershipStatus membershipStatus,
        @NotNull RiskStatus riskStatus,
        @NotNull LocalDate joiningDate,
        LocalDate exitDate,
        String remarks) {}
