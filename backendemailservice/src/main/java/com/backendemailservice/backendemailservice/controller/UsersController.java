package com.backendemailservice.backendemailservice.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;

@RestController
public class UsersController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UsersController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/deleteaccount")
    public ResponseEntity<String> deleteAccount(@RequestBody Map<String, String> payload, @RequestHeader("Authorization") String authorizationHeader) {
        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = payload.get("email");
        if (!tokenEmail.equals(userEmail)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.deleteUserAccount(userEmail);
        return ResponseEntity.ok("Account is deleted!");
    }

    @PutMapping("/changepassword")
    public ResponseEntity<String> changeUserPassword(@RequestBody Map<String, String> payload,
                                                     @RequestHeader("Authorization") String authorizationHeader) {
        String email = payload.get("email");
        String newPassword = payload.get("newPassword");

        if (email == null || email.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Email and new password are required.");
        }

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null || !tokenEmail.equals(email)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.changeUserPassword(email, newPassword);
        return ResponseEntity.ok("Password changed successfully!");
    }

    @PutMapping("/updatelanguage")
    public ResponseEntity<String> updateLanguage(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String authorizationHeader) {
        String email = request.get("email");
        String language = request.get("language");

        String tokenEmail = jwtUtil.extractAndValidateToken(authorizationHeader);
        if (tokenEmail == null || !tokenEmail.equals(email)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.updateLanguage(email, language);
        return ResponseEntity.ok("Language updated successfully!");
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