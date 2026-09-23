package com.binava.stafffinance.audit.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record AuditView(Long id, String user, String action, String entityType, Long entityId,
                 String reference, String previousStatus, String newStatus,
                 String details, Instant createdAt) {}
