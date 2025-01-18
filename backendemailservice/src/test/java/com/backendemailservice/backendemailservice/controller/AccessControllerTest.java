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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
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
    public void testSignIn_Success() throws Exception {
        User user = new User("example9@seamail.com", "nebtit");

        when(userService.findUser("example9@seamail.com", "nebtit")).thenReturn(Optional.of(user));
        
        // Dynamically generating the token to reflect how it's created in the actual implementation
        String expectedToken = jwtUtil.generateToken(user.getEmail());
        when(jwtUtil.generateToken(user.getEmail())).thenReturn(expectedToken);

        mockMvc.perform(post("/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"example9@seamail.com\",\"password\":\"nebtit\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bearer " + expectedToken));
    }

    @Test
    public void testSignIn_UserNotFound() throws Exception {
        when(userService.findUser("unknown@example.com", "wrongpassword")).thenReturn(Optional.empty());

        mockMvc.perform(post("/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    public void testSignUp_Success() throws Exception {
        User user = new User("test8@example.com", "password");
        String expectedToken = jwtUtil.generateToken(user.getEmail());

        when(userService.findUser(eq("test8@example.com"), anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(user.getEmail())).thenReturn(expectedToken);
        
        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test8@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isCreated()) // Expecting 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.token").value(expectedToken));
    }

    @Test
    public void testSignUp_UserAlreadyExists() throws Exception {
        User existingUser = new User("existing@example.com", "password");

        when(userService.findUser("existing@example.com", "password")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    public void testSignUp_InvalidInput() throws Exception {
        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email and password must not be empty"));
    }
}