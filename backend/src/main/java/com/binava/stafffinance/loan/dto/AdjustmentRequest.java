package com.binava.stafffinance.loan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record AdjustmentRequest(@NotBlank String type, @NotNull @DecimalMin("0") BigDecimal amount,
 @Min(1) @Max(120) int months, @NotNull LocalDate effectiveDate, @NotBlank @Size(max=255) String remarks) {}
