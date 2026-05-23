package com.backendemailservice.backendemailservice.service;

import com.backendemailservice.backendemailservice.dto.AuthResponseDto;
import com.backendemailservice.backendemailservice.entity.User;

import java.util.Optional;

public interface IUserService {
    // authenticate returns JWT token string
    String authenticate(String email, String password);
    // register validates domain/duplicates, creates user, returns JWT + refresh token
    AuthResponseDto register(String email, String password);
    // generate and store a refresh token in Redis, return the token string 
    String generateAndStoreRefreshToken(String email);
    // validate refresh token, issue new access token, rotate refresh token 
    AuthResponseDto refreshAccessToken(String refreshToken);
    void createUser(User user);
    Optional<User> findUser(String email, String password);
    Optional<User> foundReceiver(String email);
    void deleteUserAccount(String userEmail);
    // auth-check overload — validates that authenticatedEmail matches requestedEmail
    void deleteUserAccount(String authenticatedEmail, String requestedEmail);
    void changeUserPassword(String userEmail, String newPassword);
    // auth-check overload
    void changeUserPassword(String authenticatedEmail, String requestedEmail, String newPassword);
    void updateLanguage(String email, String language);
    // auth-check overload
    void updateLanguage(String authenticatedEmail, String requestedEmail, String language);
    boolean uploadProfilePicture(String email, byte[] profilePicture);
    byte[] fetchProfilePicture(String email);
    // auth-check overload for profile picture operations
    void uploadProfilePicture(String authenticatedEmail, String targetEmail, byte[] picture);
    byte[] fetchProfilePicture(String authenticatedEmail, String targetEmail);
    // Discord OAuth flow — returns redirect URL with JWT token
    String processDiscordOAuth(String code, String state, String allowedOrigin);
}
