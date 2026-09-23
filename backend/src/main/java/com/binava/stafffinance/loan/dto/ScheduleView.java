package com.binava.stafffinance.loan.dto;

import com.binava.stafffinance.repayment.entity.PaymentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleView(Long id, int installmentNumber, LocalDate dueDate,
                    BigDecimal expectedAmount, BigDecimal amountPaid,
                    BigDecimal remainingAmount, PaymentStatus paymentStatus, BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal closingBalance, BigDecimal openingBalance, Long scheduleVersionId) {}
