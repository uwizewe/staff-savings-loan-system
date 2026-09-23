package com.binava.stafffinance.repayment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RepaymentBatchRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
                             @NotEmpty List<@Valid RepaymentBatchItemRequest> items,
                             String remarks) {}
