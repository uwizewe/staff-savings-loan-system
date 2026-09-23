package com.binava.stafffinance.finance.dto;

import com.binava.stafffinance.finance.entity.FinanceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceRequest(@NotNull FinanceType financeType,
                      @NotNull Long categoryId,
                      @NotNull LocalDate transactionDate,
                      @NotBlank String description,
                      @NotNull @DecimalMin("0.01") BigDecimal amount,
                      String reference,
                      String supportingReference) {}
