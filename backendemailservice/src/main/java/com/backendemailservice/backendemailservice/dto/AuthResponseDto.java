package com.backendemailservice.backendemailservice.dto;

// typed response DTO for auth endpoints 
// includes refreshToken for token renewal 
public record AuthResponseDto(String accessToken, String refreshToken) {}
