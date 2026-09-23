package com.binava.stafffinance.config.dto;

import jakarta.validation.constraints.*;

public record SettingView(Long id, String key, String value, String description) {}
