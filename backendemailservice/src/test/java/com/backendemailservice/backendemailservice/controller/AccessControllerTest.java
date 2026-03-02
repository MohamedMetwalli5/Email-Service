package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @SpyBean
    private JwtUtil jwtUtil;

    @InjectMocks
    private AccessController accessController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSignIn_UserNotFound() throws Exception {
        when(userService.findUser("unknown@example.com", "wrongpassword")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    public void testSignUp_UserAlreadyExists() throws Exception {
        User existingUser = new User("existing@seamail.com", "password");

        when(userService.findUser("existing@seamail.com", "password")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@seamail.com\",\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    public void testSignUp_InvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSignUp_Success() throws Exception {
        String email = "newuser@seamail.com";
        String password = "password123";
        String mockToken = "mock-jwt-token";

        // Mock: User doesn't exist, then sign up succeeds
        when(userService.findUser(eq(email), anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(email)).thenReturn(mockToken);

        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                // Updated from isOk() to isCreated() to match production behavior (201)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.message").value("User created successfully"));
    }

    @Test
    public void testSignIn_Success() throws Exception {
        String email = "existing@seamail.com";
        String password = "password123";
        User mockUser = new User(email, password);
        String mockToken = "mock-jwt-token";

        // Mock: UserService finds the user, and JwtUtil generates the token
        when(userService.findUser(email, password)).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(email)).thenReturn(mockToken);

        mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                // Matches the "Bearer " + token logic in your controller
                .andExpect(content().string("Bearer " + mockToken));
    }
}