package com.binava.stafffinance.savings.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingRequest(@NotNull Long memberId,
                     @NotNull @DecimalMin("0.01") BigDecimal amount,
                     @NotNull LocalDate transactionDate,
                     String reference,
                     @NotBlank @Size(max=500) String description) {}
