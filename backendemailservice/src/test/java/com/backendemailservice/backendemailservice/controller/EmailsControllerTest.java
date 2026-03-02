package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class EmailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @MockBean
    private UserService userService;

    @SpyBean
    private JwtUtil jwtUtil;

    @InjectMocks
    private EmailsController emailsController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadInbox_Success() throws Exception {
        String email = "testuser@example.com";
        String token = jwtUtil.generateToken(email);

        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        
        // Setting up the email service mock
        when(emailService.loadInbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/inbox")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testLoadInbox_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/inbox"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    public void testSendEmail_Success() throws Exception {
        String email = "example1@seamail.com";
        String receiverEmail = "example2@seamail.com";
        String token = jwtUtil.generateToken(email);

        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(userService.foundReceiver(receiverEmail)).thenReturn(Optional.of(new User(receiverEmail, "password")));

        mockMvc.perform(post("/sendemail")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"" + receiverEmail + "\", \"subject\":\"Subject\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is sent!"));
    }

    @Test
    public void testLoadOutbox_Success() throws Exception {
        String email = "testuser@example.com";
        String token = jwtUtil.generateToken(email);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(emailService.loadOutbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/outbox")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testSortEmails_Authorized() throws Exception {
    String email = "test@example.com";
    String token = jwtUtil.generateToken(email);

    List<Email> sortedEmails = Arrays.asList(
        new Email("sender1@example.com", "receiver1@example.com", "Subject 1", "Body 1", "1", LocalDateTime.now(), false),
        new Email("sender2@example.com", "receiver2@example.com", "Subject 2", "Body 2", "2", LocalDateTime.now(), false)
    );

    when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
    when(emailService.sortEmails(eq(email), eq("priority"))).thenReturn(sortedEmails);

    mockMvc.perform(post("/sortemails")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sortingOption\":\"priority\"}"))
            .andExpect(status().isOk());
}

    @Test
    public void testSortEmails_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/sortemails")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"sortingOption\":\"priority\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFilterEmails_Authorized() throws Exception {
    String email = "test@example.com";
    String token = jwtUtil.generateToken(email);

    List<Email> filteredEmails = Arrays.asList(
        new Email("sender@example.com", email, "Important", "Body", "2", LocalDateTime.now(), false),
        new Email("sender2@example.com", email, "Important Update", "Body", "1", LocalDateTime.now(), false)
    );

    when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
    when(emailService.filterEmails(eq(email), eq("subject"), eq("Important"))).thenReturn(filteredEmails);

    mockMvc.perform(post("/filteremails")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
            .andExpect(status().isOk());
}

    @Test
    public void testFilterEmails_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/filteremails")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
                .andExpect(status().isUnauthorized());
    }

}