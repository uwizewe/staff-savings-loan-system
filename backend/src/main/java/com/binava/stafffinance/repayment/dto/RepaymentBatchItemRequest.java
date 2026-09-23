package com.binava.stafffinance.repayment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RepaymentBatchItemRequest(@NotNull Long loanId,
                                 @NotNull @DecimalMin("0.01") BigDecimal amount, Long scheduleId) { public RepaymentBatchItemRequest(Long loanId,BigDecimal amount) { this(loanId,amount,null); } }
