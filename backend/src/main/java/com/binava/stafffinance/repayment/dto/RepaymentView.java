package com.binava.stafffinance.repayment.dto;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RepaymentView(Long id, Long loanId, String loanNumber, Long memberId, String memberName,
                     BigDecimal amount, LocalDate paymentDate, String reference,
                     WorkflowStatus status, String createdBy, String actionedBy,
                     String remarks, Instant createdAt, Long batchId, String memberCode, Integer installmentNumber, BigDecimal expectedInstallment, BigDecimal principalPaid, BigDecimal interestPaid, BigDecimal outstandingBefore, String paymentType, BigDecimal chargesPaid) {}
