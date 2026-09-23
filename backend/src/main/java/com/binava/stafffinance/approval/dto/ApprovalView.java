package com.binava.stafffinance.approval.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record ApprovalView(String type, Long id, String reference, String description,
                    BigDecimal amount, String createdBy, Instant submittedAt) {}
