package com.backendemailservice.backendemailservice.controller;

import java.util.Map;

import com.backendemailservice.backendemailservice.dto.ChangePasswordRequestDto;
import com.backendemailservice.backendemailservice.dto.DeleteAccountRequestDto;
import com.backendemailservice.backendemailservice.dto.UpdateLanguageRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;

@RestController
@RequestMapping("/api/v1")
public class UsersController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UsersController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DeleteAccountRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authHeader);
        if (tokenEmail == null || !tokenEmail.equals(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        userService.deleteUserAccount(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Account is deleted!"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authHeader);
        if (tokenEmail == null || !tokenEmail.equals(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        userService.changeUserPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully!"));
    }

    @PutMapping("/update-language")
    public ResponseEntity<?> updateLanguage(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateLanguageRequestDto request) {

        String tokenEmail = jwtUtil.extractAndValidateToken(authHeader);
        if (tokenEmail == null || !tokenEmail.equals(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        userService.updateLanguage(request.getEmail(), request.getLanguage());
        return ResponseEntity.ok(Map.of("message", "Language updated successfully!"));
    }

    @PostMapping("/{email}/profile-picture")
    public ResponseEntity<String> uploadProfilePicture(@PathVariable String email, @RequestBody byte[] profilePicture, @RequestHeader("Authorization") String authorizationHeader) {
        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null || !tokenEmail.equals(email)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.uploadProfilePicture(email, profilePicture);
        return ResponseEntity.ok("Profile picture uploaded successfully.");
    }

    @GetMapping("/{email}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String email, @RequestHeader("Authorization") String authorizationHeader) {
        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null || !tokenEmail.equals(email)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] profilePicture = userService.fetchProfilePicture(email);
        if (profilePicture != null) {
            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .body(profilePicture);
        }
        return ResponseEntity.notFound().build();
    }
}