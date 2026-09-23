package com.binava.stafffinance.member.dto;

import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.entity.RiskStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberView(Long id, String memberCode, String fullName, String department,
                  String phone, String email, BigDecimal monthlySavingAmount,
                  MembershipStatus membershipStatus, RiskStatus riskStatus,
                  LocalDate joiningDate, LocalDate exitDate, String remarks,
                  BigDecimal totalSavings, BigDecimal outstandingLoans) {}
