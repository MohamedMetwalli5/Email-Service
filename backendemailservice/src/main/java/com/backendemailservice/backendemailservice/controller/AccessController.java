package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.dto.AuthResponseDto;
import com.backendemailservice.backendemailservice.dto.RefreshTokenRequestDto;
import com.backendemailservice.backendemailservice.dto.UserRequestDto;
import com.backendemailservice.backendemailservice.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1")
@Validated
public class AccessController {

    private final IUserService userService;

    public AccessController(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponseDto> signin(@Valid @RequestBody UserRequestDto request) {
        String accessToken = userService.authenticate(request.getEmail(), request.getPassword());
        String refreshToken = userService.generateAndStoreRefreshToken(request.getEmail());
        return ResponseEntity.ok(new AuthResponseDto(accessToken, refreshToken));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponseDto> signup(@Valid @RequestBody UserRequestDto request) {
        AuthResponseDto response = userService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(201).body(response);
    }

    // refresh token endpoint — validates refresh token, issues new access+refresh pair 
    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = userService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}
