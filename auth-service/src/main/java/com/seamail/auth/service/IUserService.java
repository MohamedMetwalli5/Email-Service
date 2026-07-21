package com.seamail.auth.service;

import com.seamail.auth.dto.AuthResponseDto;
import com.seamail.auth.dto.DiscordExchangeResponseDto;
import com.seamail.auth.entity.User;

import java.util.Optional;

public interface IUserService {
    String authenticate(String email, String password);
    AuthResponseDto register(String email, String password);
    String generateAndStoreRefreshToken(String email);
    AuthResponseDto refreshAccessToken(String refreshToken);
    void createUser(User user);
    Optional<User> findUser(String email, String password);
    Optional<User> foundReceiver(String email);
    void deleteUserAccount(String authenticatedEmail, String requestedEmail);
    void changeUserPassword(String authenticatedEmail, String requestedEmail,
                            String currentPassword, String newPassword);
    void updateLanguage(String authenticatedEmail, String requestedEmail, String language);
    boolean uploadProfilePicture(String email, byte[] profilePicture);
    void uploadProfilePicture(String authenticatedEmail, String targetEmail, byte[] picture);
    byte[] fetchProfilePicture(String authenticatedEmail, String targetEmail);
    String processDiscordOAuth(String code, String state, String allowedOrigin);
    String generateDiscordState();
    void validateDiscordState(String state);
    DiscordExchangeResponseDto exchangeDiscordTicket(String ticket);
}
