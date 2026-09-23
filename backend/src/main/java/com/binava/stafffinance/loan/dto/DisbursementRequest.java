package com.binava.stafffinance.loan.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DisbursementRequest(@NotNull LocalDate disbursementDate, @NotBlank String reference) {}
