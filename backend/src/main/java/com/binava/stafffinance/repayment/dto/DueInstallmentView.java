package com.binava.stafffinance.repayment.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record DueInstallmentView(Long scheduleId,Long loanId,String loanNumber,String memberCode,String memberName,
 int installmentNumber,LocalDate dueDate,BigDecimal principal,BigDecimal interest,BigDecimal amount,BigDecimal outstandingBalance) {}
