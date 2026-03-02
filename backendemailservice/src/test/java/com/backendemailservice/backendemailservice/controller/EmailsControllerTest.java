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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) // Added to prevent security filter chain interference
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

    private final String dummyToken = "Bearer mock.token.here";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadInbox_Success() throws Exception {
        String email = "testuser@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());
        when(emailService.loadInbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/inbox")
                .header("Authorization", dummyToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testLoadInbox_Unauthorized_NoToken() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(any());

        mockMvc.perform(get("/api/v1/inbox")
                .header("Authorization", dummyToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSendEmail_Success() throws Exception {
        String senderEmail = "example1@seamail.com";
        String receiverEmail = "example2@seamail.com";

        doReturn(senderEmail).when(jwtUtil).extractAndValidateToken(anyString());
        when(userService.foundReceiver(receiverEmail)).thenReturn(Optional.of(new User(receiverEmail, "password")));

        mockMvc.perform(post("/api/v1/send-email")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"" + receiverEmail + "\", \"subject\":\"Subject\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is sent!"));

        verify(emailService, times(1)).createEmail(any(Email.class));
    }

    @Test
    public void testLoadOutbox_Success() throws Exception {
        String email = "testuser@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());
        when(emailService.loadOutbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/outbox")
                .header("Authorization", dummyToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testSortEmails_Authorized() throws Exception {
        String email = "test@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        List<Email> sortedEmails = Arrays.asList(
            new Email("sender1@seamail.com", "receiver1@seamail.com", "Subject 1", "Body 1", "1", LocalDateTime.now(), false)
        );

        when(emailService.sortEmails(eq(email), eq("priority"))).thenReturn(sortedEmails);

        mockMvc.perform(post("/api/v1/sort-emails")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sortingOption\":\"priority\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSortEmails_Unauthorized_NoToken() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(any());

        mockMvc.perform(post("/api/v1/sort-emails")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sortingOption\":\"priority\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFilterEmails_Authorized() throws Exception {
        String email = "test@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        List<Email> filteredEmails = Arrays.asList(
            new Email("sender@seamail.com", email, "Important", "Body", "2", LocalDateTime.now(), false)
        );

        when(emailService.filterEmails(eq(email), eq("subject"), eq("Important"))).thenReturn(filteredEmails);

        mockMvc.perform(post("/api/v1/filter-emails")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testFilterEmails_Unauthorized_NoToken() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(any());

        mockMvc.perform(post("/api/v1/filter-emails")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoadTrashbox_Success() throws Exception {
        String email = "testuser@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());
        when(emailService.loadTrashbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/trashbox")
                .header("Authorization", dummyToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testMoveEmailToTrash_Success() throws Exception {
        String email = "test@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(post("/api/v1/move-to-trash")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Moved email to trashbox!"));

        verify(emailService, times(1)).moveToTrashBox((int) 1L);
    }

    @Test
    public void testDeleteEmail_Success() throws Exception {
        String email = "test@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(delete("/api/v1/delete-email")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is deleted!"));

        verify(emailService, times(1)).deleteEmail((int) 100L);
    }

    @Test
    public void testSendEmail_ReceiverNotFound() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "nonexistent@seamail.com";

        doReturn(senderEmail).when(jwtUtil).extractAndValidateToken(anyString());
        when(userService.foundReceiver(receiverEmail)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/send-email")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"" + receiverEmail + "\", \"subject\":\"Hi\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isNotFound());
    }
}