package com.binava.stafffinance.savings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SavingsBatchRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
                           @NotEmpty List<@Valid SavingsBatchItemRequest> items,
                           String remarks) {}
