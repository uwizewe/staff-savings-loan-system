package com.binava.stafffinance.common.dto;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record BatchView(Long id, String period, BigDecimal totalAmount, WorkflowStatus status,
                 String createdBy, String actionedBy, Instant createdAt, String remarks) {}
