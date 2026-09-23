package com.binava.stafffinance.repayment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentRequest(@NotNull Long loanId,
                        @NotNull @DecimalMin("0.01") BigDecimal amount,
                        @NotNull LocalDate paymentDate,
                        String reference,
                        String remarks, Long scheduleId, String paymentType) {
    public RepaymentRequest(Long loanId,BigDecimal amount,LocalDate paymentDate,String reference,String remarks) { this(loanId,amount,paymentDate,reference,remarks,null,"GENERAL"); }
}
