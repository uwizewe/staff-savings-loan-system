package com.binava.stafffinance.finance.dto;

import com.binava.stafffinance.finance.entity.CategoryType;
import jakarta.validation.constraints.*;

public record CategoryRequest(@NotBlank String name, @NotNull CategoryType categoryType,
                       Boolean active) {}
