package com.binava.stafffinance.user.dto;

import com.binava.stafffinance.role.Role;
import jakarta.validation.constraints.*;

public record UserView(Long id, String username, String fullName, String email, Role role,
                boolean enabled, Long memberId) {}
