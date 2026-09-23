package com.binava.stafffinance.savings.dto;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.savings.entity.SavingType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SavingView(Long id, Long memberId, String memberName, SavingType savingType,
                  BigDecimal amount, LocalDate transactionDate, String reference,
                  String description, WorkflowStatus status, String createdBy,
                  String actionedBy, Instant createdAt, Long batchId) {}
