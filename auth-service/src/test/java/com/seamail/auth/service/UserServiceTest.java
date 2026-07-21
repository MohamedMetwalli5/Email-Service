package com.seamail.auth.service;

import com.seamail.auth.config.DiscordOAuthProperties;
import com.seamail.auth.dto.AuthResponseDto;
import com.seamail.auth.dto.DiscordExchangeResponseDto;
import com.seamail.auth.entity.User;
import com.seamail.auth.exception.InvalidEmailDomainException;
import com.seamail.auth.exception.InvalidFileFormatException;
import com.seamail.auth.exception.UserAlreadyExistsException;
import com.seamail.auth.exception.UserNotFoundException;
import com.seamail.auth.repository.UserRepository;
import com.seamail.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit test using MockitoExtension instead of @SpringBootTest
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtUtil;

    @Mock
    private DiscordOAuthProperties discordOAuthProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserService userService;

    // --- authenticate ---

    @Test
    void shouldThrowUserNotFoundWhenEmailDoesNotExistOnAuthenticate() {
        String email = "ghost@seamail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userService.authenticate(email, "anyPassword"));

        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void shouldThrowUserNotFoundWhenPasswordDoesNotMatch() {
        String email = "user@seamail.com";
        User user = new User(email, "encodedPassword");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userService.authenticate(email, "wrongPassword"));

        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() {
        String email = "user@seamail.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword";
        String expectedToken = "jwt-token";

        User user = new User(email, encodedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(email)).thenReturn(expectedToken);

        String token = userService.authenticate(email, rawPassword);

        assertEquals(expectedToken, token);
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(jwtUtil).generateToken(email);
    }

    // --- register ---

    @Test
    void shouldThrowUserAlreadyExistsWhenRegisteringDuplicateEmail() {
        String email = "existing@seamail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User(email, "pass")));

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.register(email, "password123"));

        assertEquals("USER_ALREADY_EXISTS", ex.getErrorCode());
        verify(userRepository).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void shouldThrowInvalidEmailDomainWhenDomainIsNotSeamail() {
        String email = "user@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        InvalidEmailDomainException ex = assertThrows(InvalidEmailDomainException.class,
                () -> userService.register(email, "password123"));

        assertEquals("INVALID_EMAIL_DOMAIN", ex.getErrorCode());
        verify(userRepository).findByEmail(email);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void shouldReturnAuthResponseDtoWhenRegistrationSucceeds() {
        String email = "newuser@seamail.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword";
        String expectedToken = "jwt-token";
        String expectedRefreshToken = "refresh-token";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(jwtUtil.generateToken(email)).thenReturn(expectedToken);
        when(jwtUtil.generateRefreshToken()).thenReturn(expectedRefreshToken);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponseDto response = userService.register(email, rawPassword);

        assertEquals(expectedToken, response.accessToken());
        assertEquals(expectedRefreshToken, response.refreshToken());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(email);
        verify(jwtUtil).generateRefreshToken();
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq("refresh_token:" + expectedRefreshToken), eq(email), eq(7L), eq(TimeUnit.DAYS));
    }

    // --- findUser ---

    @Test
    void shouldFindUserWhenCredentialsMatch() {
        String email = "user@seamail.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword";
        User user = new User(email, encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        Optional<User> result = userService.findUser(email, rawPassword);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        String email = "unknown@seamail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.findUser(email, "anyPassword");

        assertFalse(result.isPresent());
        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder);
    }

    // --- foundReceiver ---

    @Test
    void shouldReturnUserWhenReceiverFound() {
        String email = "receiver@seamail.com";
        User user = new User(email, "pass");
        when(userRepository.foundReceiver(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.foundReceiver(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository).foundReceiver(email);
    }

    // --- deleteUserAccount (auth-check overload) ---

    @Test
    void shouldThrowWhenAuthenticatedEmailDoesNotMatchOnDelete() {
        String authenticatedEmail = "user1@seamail.com";
        String requestedEmail = "user2@seamail.com";

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> userService.deleteUserAccount(authenticatedEmail, requestedEmail));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldDeleteUserAccountWhenAuthenticatedEmailMatches() {
        String email = "user@seamail.com";
        userService.deleteUserAccount(email, email);
        verify(userRepository).deleteById(email);
    }

    @Test
    void shouldRevokeRefreshTokensWhenDeletingAccount() {
        String email = "user@seamail.com";
        String tokenKey1 = "refresh_token:token-1";
        String tokenKey2 = "refresh_token:token-2";

        when(redisTemplate.keys("refresh_token:*")).thenReturn(Set.of(tokenKey1, tokenKey2, "refresh_token:other-user-token"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey1)).thenReturn(email);
        when(valueOperations.get(tokenKey2)).thenReturn(email);
        when(valueOperations.get("refresh_token:other-user-token")).thenReturn("other@seamail.com");

        userService.deleteUserAccount(email, email);

        verify(redisTemplate).delete(tokenKey1);
        verify(redisTemplate).delete(tokenKey2);
        verify(redisTemplate, never()).delete("refresh_token:other-user-token");
        verify(userRepository).deleteById(email);
    }

    // --- changeUserPassword ---

    @Test
    void shouldChangeUserPasswordWhenCurrentPasswordMatches() {
        String email = "user@seamail.com";
        String currentPassword = "oldPassword123";
        String newPassword = "newPassword123";
        String encodedCurrent = "encodedOldPassword";
        String encodedNew = "encodedNewPassword123";
        User user = new User(email, encodedCurrent);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, encodedCurrent)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNew);

        userService.changeUserPassword(email, email, currentPassword, newPassword);

        assertEquals(encodedNew, user.getPassword());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(currentPassword, encodedCurrent);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenCurrentPasswordDoesNotMatch() {
        String email = "user@seamail.com";
        User user = new User(email, "encodedOldPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongCurrent", "encodedOldPassword")).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> userService.changeUserPassword(email, email, "wrongCurrent", "newPassword123"));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRevokeRefreshTokensWhenChangingPassword() {
        String email = "user@seamail.com";
        String currentPassword = "oldPassword123";
        String newPassword = "newPassword123";
        User user = new User(email, "encodedOldPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNew");
        when(redisTemplate.keys("refresh_token:*")).thenReturn(Set.of("refresh_token:tok-1", "refresh_token:tok-2"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:tok-1")).thenReturn(email);
        when(valueOperations.get("refresh_token:tok-2")).thenReturn("other@seamail.com");

        userService.changeUserPassword(email, email, currentPassword, newPassword);

        verify(redisTemplate).delete("refresh_token:tok-1");
        verify(redisTemplate, never()).delete("refresh_token:tok-2");
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowUserNotFoundWhenChangingPasswordForUnknownUser() {
        String email = "unknown@seamail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userService.changeUserPassword(email, email, "current", "newPassword123"));

        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder);
    }

    // --- updateLanguage ---

    @Test
    void shouldUpdateLanguageWhenUserExists() {
        String email = "user@seamail.com";
        String language = "fr";
        User user = new User(email, "pass");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userService.updateLanguage(email, email, language);

        assertEquals(language, user.getLanguage());
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(user);
    }

    // --- uploadProfilePicture ---

    @Test
    void shouldAcceptValidPngImage() {
        String email = "user@seamail.com";
        byte[] validPng = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0};
        User user = new User(email, "pass");

        when(userRepository.findById(email)).thenReturn(Optional.of(user));

        boolean result = userService.uploadProfilePicture(email, validPng);

        assertTrue(result);
        assertArrayEquals(validPng, user.getProfilePicture());
        verify(userRepository).findById(email);
        verify(userRepository).save(user);
    }

    @Test
    void shouldAcceptValidJpegImage() {
        String email = "user@seamail.com";
        byte[] validJpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0};
        User user = new User(email, "pass");

        when(userRepository.findById(email)).thenReturn(Optional.of(user));

        boolean result = userService.uploadProfilePicture(email, validJpeg);

        assertTrue(result);
        assertArrayEquals(validJpeg, user.getProfilePicture());
        verify(userRepository).findById(email);
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectInvalidFileFormat() {
        String email = "user@seamail.com";
        byte[] invalidData = {1, 2, 3, 4, 5, 6, 7, 8};

        assertThrows(InvalidFileFormatException.class,
                () -> userService.uploadProfilePicture(email, invalidData));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectFileExceedingSizeLimit() {
        String email = "user@seamail.com";
        byte[] largeFile = new byte[6 * 1024 * 1024];

        assertThrows(InvalidFileFormatException.class,
                () -> userService.uploadProfilePicture(email, largeFile));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectNullProfilePicture() {
        String email = "user@seamail.com";

        assertThrows(InvalidFileFormatException.class,
                () -> userService.uploadProfilePicture(email, null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowUserNotFoundWhenUploadingPictureForUnknownUser() {
        String email = "unknown@seamail.com";
        byte[] validPng = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0};

        when(userRepository.findById(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userService.uploadProfilePicture(email, validPng));

        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        verify(userRepository).findById(email);
    }

    // --- fetchProfilePicture ---

    @Test
    void shouldReturnProfilePictureWhenUserExists() {
        String email = "user@seamail.com";
        byte[] picture = {1, 2, 3, 4, 5};
        User user = new User(email, "pass");
        user.setProfilePicture(picture);

        when(userRepository.findById(email)).thenReturn(Optional.of(user));

        byte[] result = userService.fetchProfilePicture(email, email);

        assertArrayEquals(picture, result);
        verify(userRepository).findById(email);
    }

    @Test
    void shouldThrowUserNotFoundWhenFetchingPictureForUnknownUser() {
        String email = "unknown@seamail.com";
        when(userRepository.findById(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userService.fetchProfilePicture(email, email));

        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        verify(userRepository).findById(email);
    }

    // --- generateAndStoreRefreshToken ---

    @Test
    void shouldGenerateAndStoreRefreshToken() {
        String email = "user@seamail.com";
        String expectedToken = "uuid-refresh-token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.generateRefreshToken()).thenReturn(expectedToken);

        String result = userService.generateAndStoreRefreshToken(email);

        assertEquals(expectedToken, result);
        verify(redisTemplate.opsForValue()).set(
                "refresh_token:" + expectedToken, email, 7, TimeUnit.DAYS);
    }

    // --- refreshAccessToken ---

    @Test
    void shouldThrowWhenRefreshTokenNotFound() {
        String refreshToken = "invalid-or-expired";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + refreshToken)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> userService.refreshAccessToken(refreshToken));
    }

    @Test
    void shouldRotateTokensWhenRefreshTokenIsValid() {
        String oldRefreshToken = "valid-refresh-token";
        String email = "user@seamail.com";
        String newAccessToken = "new-jwt-token";
        String newRefreshToken = "new-uuid-refresh-token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + oldRefreshToken)).thenReturn(email);
        when(redisTemplate.delete("refresh_token:" + oldRefreshToken)).thenReturn(true);
        when(jwtUtil.generateToken(email)).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken()).thenReturn(newRefreshToken);

        AuthResponseDto result = userService.refreshAccessToken(oldRefreshToken);

        assertEquals(newAccessToken, result.accessToken());
        assertEquals(newRefreshToken, result.refreshToken());
        verify(redisTemplate).delete("refresh_token:" + oldRefreshToken);
        verify(redisTemplate.opsForValue()).set(
                "refresh_token:" + newRefreshToken, email, 7, TimeUnit.DAYS);
    }

    @Test
    void shouldRejectWhenRefreshTokenAlreadyClaimedConcurrently() {
        String oldRefreshToken = "valid-refresh-token";
        String email = "user@seamail.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + oldRefreshToken)).thenReturn(email);
        when(redisTemplate.delete("refresh_token:" + oldRefreshToken)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> userService.refreshAccessToken(oldRefreshToken));

        verify(jwtUtil, never()).generateToken(any());
        verify(jwtUtil, never()).generateRefreshToken();
    }

    // --- exchangeDiscordTicket ---
    // Note: processDiscordOAuth itself makes live RestTemplate calls to Discord
    // and is not unit-testable without extracting that client (a pre-existing gap).
    // These tests cover the ticket-exchange contract, which is the security-critical
    // half of the fix: tokens are never in the URL, only an opaque single-use code.

    // --- Discord state validation (CSRF) ---

    @Test
    void shouldGenerateAndStoreDiscordState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String state = userService.generateDiscordState();

        assertNotNull(state);
        verify(redisTemplate.opsForValue()).set(
                eq("discord_state:" + state), eq(state), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldThrowWhenDiscordStateIsInvalid() {
        String state = "invalid-state";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_state:" + state)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> userService.validateDiscordState(state));
    }

    @Test
    void shouldAcceptAndDeleteValidDiscordState() {
        String state = "valid-state";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_state:" + state)).thenReturn(state);
        when(redisTemplate.delete("discord_state:" + state)).thenReturn(true);

        userService.validateDiscordState(state);

        verify(redisTemplate).delete("discord_state:" + state);
    }

    @Test
    void shouldExchangeValidTicketForTokens() {
        String ticket = "valid-ticket";
        String email = "discorduser@seamail.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_ticket_result:" + ticket)).thenReturn(null);
        when(valueOperations.get("discord_ticket:" + ticket)).thenReturn(email);
        when(redisTemplate.delete("discord_ticket:" + ticket)).thenReturn(true);
        when(jwtUtil.generateToken(email)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh");

        DiscordExchangeResponseDto result = userService.exchangeDiscordTicket(ticket);

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertEquals(email, result.email());
        verify(redisTemplate).delete("discord_ticket:" + ticket);
    }

    @Test
    void shouldThrowWhenExchangingInvalidOrExpiredTicket() {
        String ticket = "unknown-ticket";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_ticket_result:" + ticket)).thenReturn(null);
        when(valueOperations.get("discord_ticket:" + ticket)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> userService.exchangeDiscordTicket(ticket));

        verify(jwtUtil, never()).generateToken(any());
        verify(jwtUtil, never()).generateRefreshToken();
    }

    @Test
    void shouldRejectWhenDiscordTicketAlreadyClaimedConcurrently() {
        String ticket = "race-ticket";
        String email = "discorduser@seamail.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_ticket_result:" + ticket)).thenReturn(null);
        when(valueOperations.get("discord_ticket:" + ticket)).thenReturn(email);
        when(redisTemplate.delete("discord_ticket:" + ticket)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> userService.exchangeDiscordTicket(ticket));

        verify(jwtUtil, never()).generateToken(any());
        verify(jwtUtil, never()).generateRefreshToken();
    }

    @Test
    void shouldReturnCachedResultWhenTicketIsExchangedAgain() {
        String ticket = "valid-ticket";
        String cachedJson = "{\"accessToken\":\"cached-access\",\"refreshToken\":\"cached-refresh\",\"email\":\"discorduser@seamail.com\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("discord_ticket_result:" + ticket)).thenReturn(cachedJson);

        DiscordExchangeResponseDto result = userService.exchangeDiscordTicket(ticket);

        assertEquals("cached-access", result.accessToken());
        assertEquals("cached-refresh", result.refreshToken());
        assertEquals("discorduser@seamail.com", result.email());
        verify(jwtUtil, never()).generateToken(any());
        verify(jwtUtil, never()).generateRefreshToken();
        verify(redisTemplate, never()).delete(anyString());
    }
}
