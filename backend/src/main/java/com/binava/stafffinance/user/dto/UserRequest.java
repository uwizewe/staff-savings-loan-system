package com.binava.stafffinance.user.dto;

import com.binava.stafffinance.role.Role;
import jakarta.validation.constraints.*;

public record UserRequest(@NotBlank String username, @NotBlank String fullName,
                   @Email @NotBlank String email, @NotNull Role role,
                   String password, Boolean enabled, Long memberId) {}
