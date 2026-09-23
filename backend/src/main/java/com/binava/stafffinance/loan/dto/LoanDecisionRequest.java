package com.binava.stafffinance.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LoanDecisionRequest(String remarks, @DecimalMin("1.00") BigDecimal approvedAmount,
                           @DecimalMin("0.00") BigDecimal annualInterestRate,
                           @Min(1) @Max(120) Integer repaymentMonths) {}
