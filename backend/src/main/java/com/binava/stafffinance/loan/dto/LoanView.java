package com.binava.stafffinance.loan.dto;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.loan.entity.LoanStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoanView(Long id, String applicationNumber, Long memberId, String memberName,
                LocalDate applicationDate, BigDecimal requestedAmount, BigDecimal approvedAmount,
                BigDecimal annualInterestRate, int repaymentMonths, String purpose,
                BigDecimal monthlyInstallment, BigDecimal totalInterest, BigDecimal totalPayable,
                BigDecimal amountRepaid, BigDecimal outstandingBalance, WorkflowStatus workflowStatus,
                LoanStatus loanStatus, LocalDate disbursementDate, String disbursementReference,
                String createdBy, String actionedBy, String remarks, Instant createdAt, String categoryName, String interestMethod, String adjustmentType, BigDecimal adjustmentAmount, Integer adjustmentMonths, LocalDate adjustmentDate, String adjustmentRequestedName, BigDecimal paidPrincipal, BigDecimal paidInterest, String adjustmentRemarks, String memberCode, BigDecimal originalPrincipal, BigDecimal currentPrincipal, BigDecimal remainingPrincipal, Integer originalTerm, LocalDate firstInstallmentDate, Long currentScheduleVersionId, int installmentsPaid, int installmentsRemaining) {}
