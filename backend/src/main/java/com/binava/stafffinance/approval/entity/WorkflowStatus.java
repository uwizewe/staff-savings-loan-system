package com.binava.stafffinance.approval.entity;

import jakarta.persistence.*;

public enum WorkflowStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, REVERSED }
