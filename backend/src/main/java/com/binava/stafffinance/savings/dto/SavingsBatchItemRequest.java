package com.binava.stafffinance.savings.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SavingsBatchItemRequest(@NotNull Long memberId,
                               @NotNull @DecimalMin("0.01") BigDecimal amount) {}
