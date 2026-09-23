package com.binava.stafffinance.approval.dto;

import jakarta.validation.constraints.*;

public record DecisionRequest(boolean approve, String remarks) {}
