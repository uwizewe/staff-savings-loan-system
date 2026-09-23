package com.binava.stafffinance.auth.dto;

import com.binava.stafffinance.user.dto.UserView;
import jakarta.validation.constraints.*;

public record LoginResponse(String token, UserView user) {}
