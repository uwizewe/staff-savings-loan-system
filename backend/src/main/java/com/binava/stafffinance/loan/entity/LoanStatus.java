package com.binava.stafffinance.loan.entity;

import jakarta.persistence.*;

public enum LoanStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, ACTIVE, COMPLETED, CLOSED }
