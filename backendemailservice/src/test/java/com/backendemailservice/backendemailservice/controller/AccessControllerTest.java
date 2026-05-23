package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.exception.UserAlreadyExistsException;
import com.backendemailservice.backendemailservice.exception.UserNotFoundException;
import com.backendemailservice.backendemailservice.config.TestSecurityConfig;
import com.backendemailservice.backendemailservice.dto.AuthResponseDto;
import com.backendemailservice.backendemailservice.service.IUserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Slice test using @WebMvcTest instead of @SpringBootTest
// @WebMvcTest is now used, removing the unused import pattern
@WebMvcTest(AccessController.class)
@Import(TestSecurityConfig.class)
class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void shouldReturn404WhenUserNotFoundOnSignIn() throws Exception {
        when(userService.authenticate("unknown@example.com", "wrongpassword"))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldReturn409WhenUserAlreadyExistsOnSignUp() throws Exception {
        when(userService.register("existing@seamail.com", "password"))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@seamail.com\",\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"))
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturn400WhenEmailIsBlankOnSignUp() throws Exception {
        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn400WhenEmailIsBlankOnSignIn() throws Exception {
        mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn201AndTokensWhenSignUpSucceeds() throws Exception {
        String email = "newuser@seamail.com";
        String password = "password123";
        String mockToken = "mock-jwt-token";
        String mockRefreshToken = "mock-refresh-token";

        when(userService.register(email, password)).thenReturn(new AuthResponseDto(mockToken, mockRefreshToken));

        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(mockToken))
                .andExpect(jsonPath("$.refreshToken").value(mockRefreshToken));
    }

    @Test
    void shouldReturn200AndTokensWhenSignInSucceeds() throws Exception {
        String email = "existing@seamail.com";
        String password = "password123";
        String mockToken = "mock-jwt-token";
        String mockRefreshToken = "mock-refresh-token";

        when(userService.authenticate(email, password)).thenReturn(mockToken);
        when(userService.generateAndStoreRefreshToken(email)).thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(mockToken))
                .andExpect(jsonPath("$.refreshToken").value(mockRefreshToken));
    }

    // refresh token endpoint tests
    @Test
    void shouldReturn200WhenRefreshTokenIsValid() throws Exception {
        String refreshToken = "valid-refresh-token";
        AuthResponseDto mockResponse = new AuthResponseDto("new-access-token", "new-refresh-token");

        when(userService.refreshAccessToken(refreshToken)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}
