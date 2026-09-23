package com.binava.stafffinance.config.dto;

import jakarta.validation.constraints.*;

public record SettingRequest(@NotBlank String value) {}
