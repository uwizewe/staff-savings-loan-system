package com.binava.stafffinance.loan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CategoryRequest(@NotBlank @Size(max=100) String name, @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal annualRate, @Size(max=1000) String description, Boolean active) {}
