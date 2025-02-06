package com.backendemailservice.backendemailservice.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;


@RestController
public class UsersController {
	
	@Value("${cors.allowed.origin}")
    private String allowedOrigin;
	
    private final EmailService emailService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UsersController(EmailService emailService, UserService userService, JwtUtil jwtUtil) {
        this.emailService = emailService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/deleteaccount")
    public ResponseEntity<String> deleteAccount(@RequestBody Map<String, String> payload, @RequestHeader("Authorization") String authorizationHeader) {
        String userEmail = payload.get("email");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token, userEmail)) {
            System.out.println("Token validation failed for user: " + userEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.deleteUserAccount(userEmail);
        return ResponseEntity.ok("Email is deleted!");
    }
    
    @PutMapping("/changepassword")
    public ResponseEntity<String> changeUserPassword(@RequestBody Map<String, String> payload, 
                                                     @RequestHeader("Authorization") String authorizationHeader) {
    	String email = payload.get("email");
        String newPassword = payload.get("newPassword");

        if (email == null || email.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Email and new password are required.");
        }
    	
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        
        if (!jwtUtil.isTokenValid(token, email)) {
            System.out.println("Token validation failed for user: " + email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean success = userService.changeUserPassword(email, newPassword);
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
            return ResponseEntity.ok("Password changed successfully!");
        } catch (Exception e) {
            System.err.println("Error changing password for user: " + email);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to change password. Please try again later.");
        }
    }
    
    @PutMapping("/updatelanguage")
    public ResponseEntity<String> updateLanguage(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String token) {
        String language = request.get("language");
        String email = request.get("email");
        
        try {
            boolean success = userService.updateLanguage(email, language);
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
            return ResponseEntity.ok("Language updated successfully!");
        } catch (Exception e) {
            System.err.println("Error updating language for user: " + email);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update language. Please try again later.");
        }
    }
    

    @PostMapping("/{email}/profile-picture")
    public ResponseEntity<String> uploadProfilePicture(@PathVariable String email, @RequestBody byte[] profilePicture) {
    	boolean isUploaded = userService.uploadProfilePicture(email, profilePicture);
        return isUploaded
                ? ResponseEntity.ok("Profile picture uploaded successfully.")
                : ResponseEntity.badRequest().body("Failed to upload profile picture.");
    }
    
    @GetMapping("/{email}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String email) {
        byte[] profilePicture = userService.fetchProfilePicture(email);

        if (profilePicture != null) {
            return ResponseEntity
                    .ok()
                    .header("Content-Type", "image/png")
                    .body(profilePicture);
        }
        return ResponseEntity.notFound().build();
    }
    
}
