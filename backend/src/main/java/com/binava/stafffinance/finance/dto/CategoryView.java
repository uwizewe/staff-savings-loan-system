package com.binava.stafffinance.finance.dto;

import com.binava.stafffinance.finance.entity.CategoryType;
import jakarta.validation.constraints.*;

public record CategoryView(Long id, String name, CategoryType categoryType, boolean active) {}
