package com.binava.stafffinance.finance.dto;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.finance.entity.FinanceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FinanceView(Long id, String reference, LocalDate transactionDate,
                   FinanceType financeType, Long categoryId, String category,
                   String description, BigDecimal amount, String supportingReference,
                   WorkflowStatus status, String createdBy, String actionedBy,
                   Instant createdAt) {}
