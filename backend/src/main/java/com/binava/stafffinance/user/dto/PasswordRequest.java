package com.binava.stafffinance.user.dto;

import jakarta.validation.constraints.*;

public record PasswordRequest(@NotBlank @Size(min = 8) String password) {}
