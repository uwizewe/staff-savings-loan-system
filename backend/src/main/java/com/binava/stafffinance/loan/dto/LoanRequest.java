package com.binava.stafffinance.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanRequest(@NotNull Long memberId,
                   @NotNull LocalDate applicationDate,
                   @NotNull @DecimalMin("1.00") BigDecimal requestedAmount,
                   @NotNull @DecimalMin("0.00") @DecimalMax("100") BigDecimal annualInterestRate,
                   @Min(1) @Max(120) int repaymentMonths,
                   @NotBlank String purpose,
                   String remarks, LocalDate firstInstallmentDate) {
    public LoanRequest(Long memberId, LocalDate applicationDate, BigDecimal requestedAmount,
            BigDecimal annualInterestRate, int repaymentMonths, String purpose, String remarks) {
        this(memberId, applicationDate, requestedAmount, annualInterestRate, repaymentMonths, purpose, remarks, applicationDate.plusMonths(1));
    }
    public LocalDate firstInstallmentDate() { return firstInstallmentDate == null ? applicationDate.plusMonths(1) : firstInstallmentDate; }
}
