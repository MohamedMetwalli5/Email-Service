package com.seamail.auth.controller;

import com.seamail.auth.dto.ChangePasswordRequestDto;
import com.seamail.auth.dto.DeleteAccountRequestDto;
import com.seamail.auth.dto.UpdateLanguageRequestDto;
import com.seamail.auth.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1")
@Validated
public class UsersController {

    private final IUserService userService;

    public UsersController(IUserService userService) {
        this.userService = userService;
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal(expression = "subject") String authenticatedEmail,
            @Valid @RequestBody DeleteAccountRequestDto request) {
        userService.deleteUserAccount(authenticatedEmail, request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal(expression = "subject") String authenticatedEmail,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        userService.changeUserPassword(authenticatedEmail, request.getEmail(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update-language")
    public ResponseEntity<Void> updateLanguage(
            @AuthenticationPrincipal(expression = "subject") String authenticatedEmail,
            @Valid @RequestBody UpdateLanguageRequestDto request) {
        userService.updateLanguage(authenticatedEmail, request.getEmail(),
                request.getLanguage());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{email}/profile-picture")
    public ResponseEntity<Void> uploadProfilePicture(
            @AuthenticationPrincipal(expression = "subject") String authenticatedEmail,
            @PathVariable String email,
            @RequestBody byte[] picture) {
        userService.uploadProfilePicture(authenticatedEmail, email, picture);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{email}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(
            @AuthenticationPrincipal(expression = "subject") String authenticatedEmail,
            @PathVariable String email) {
        byte[] picture = userService.fetchProfilePicture(authenticatedEmail, email);
        if (picture == null || picture.length == 0) {
            return ResponseEntity.notFound().build();
        }
        String contentType = (picture[0] == (byte) 0x89 && picture[1] == (byte) 0x50)
                ? "image/png" : "image/jpeg";
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(picture);
    }
}
