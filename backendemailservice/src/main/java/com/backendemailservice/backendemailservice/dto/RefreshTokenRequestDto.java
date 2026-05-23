package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotBlank;

// request body for POST /api/v1/auth/refresh
public record RefreshTokenRequestDto(
        @NotBlank String refreshToken
) {}
