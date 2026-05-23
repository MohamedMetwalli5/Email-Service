package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.config.TestSecurityConfig;
import com.backendemailservice.backendemailservice.dto.EmailResponseDto;
import com.backendemailservice.backendemailservice.dto.SendEmailRequestDto;
import com.backendemailservice.backendemailservice.exception.ReceiverNotFoundException;
import com.backendemailservice.backendemailservice.service.IEmailService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// V-26: Slice test using @WebMvcTest instead of @SpringBootTest (J-7, MV-1)
@WebMvcTest(EmailsController.class)
@Import(TestSecurityConfig.class)
class EmailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEmailService emailService;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String TEST_EMAIL = "testuser@seamail.com";

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_EMAIL, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    // --- Load inbox/outbox/trashbox ---

    @Test
    void shouldReturn200WhenLoadingInbox() throws Exception {
        when(emailService.loadInboxDtos(TEST_EMAIL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/inbox"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn200WhenLoadingOutbox() throws Exception {
        when(emailService.loadOutboxDtos(TEST_EMAIL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/outbox"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn200WhenLoadingTrashbox() throws Exception {
        when(emailService.loadTrashboxDtos(TEST_EMAIL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/trashbox"))
                .andExpect(status().isOk());
    }

    // --- Send email ---

    @Test
    void shouldReturn201WhenSendingEmail() throws Exception {
        String receiverEmail = "example2@seamail.com";

        mockMvc.perform(post("/api/v1/send-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"" + receiverEmail
                        + "\", \"subject\":\"Subject\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isCreated());

        verify(emailService).sendEmail(eq(TEST_EMAIL), any(SendEmailRequestDto.class));
    }

    @Test
    void shouldReturn404WhenReceiverNotFoundOnSendEmail() throws Exception {
        doThrow(new ReceiverNotFoundException("Receiver not found"))
                .when(emailService).sendEmail(eq(TEST_EMAIL), any(SendEmailRequestDto.class));

        mockMvc.perform(post("/api/v1/send-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"nonexistent@seamail.com\", \"subject\":\"Hi\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECEIVER_NOT_FOUND"));
    }

    // --- Move to trash ---

    @Test
    void shouldReturn204WhenMovingEmailToTrash() throws Exception {
        mockMvc.perform(post("/api/v1/move-to-trash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":1}"))
                .andExpect(status().isNoContent());

        verify(emailService).moveToTrashBox(1L, TEST_EMAIL);
    }

    // --- Delete email ---

    @Test
    void shouldReturn204WhenDeletingEmail() throws Exception {
        mockMvc.perform(delete("/api/v1/delete-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":100}"))
                .andExpect(status().isNoContent());

        verify(emailService).deleteEmail(100L, TEST_EMAIL);
    }

    // --- Query emails (sort/filter as GET) ---

    @Test
    void shouldReturn200WhenQueryingEmailsBySort() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<EmailResponseDto> sorted = List.of(
                new EmailResponseDto(1L, "a@b.com", TEST_EMAIL, "S", "B", "1", now, false)
        );

        when(emailService.queryEmails(eq(TEST_EMAIL), eq("priority"), eq(null), eq(null), eq(null)))
                .thenReturn(sorted);

        mockMvc.perform(get("/api/v1/emails")
                .param("sort", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("S"));
    }

    @Test
    void shouldReturn200WhenFilteringEmailsBySubject() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<EmailResponseDto> filtered = List.of(
                new EmailResponseDto(2L, "boss@b.com", TEST_EMAIL, "Invoice", "B", "2", now, false)
        );

        when(emailService.queryEmails(eq(TEST_EMAIL), eq(null), eq("subject"), eq("Invoice"), eq(null)))
                .thenReturn(filtered);

        mockMvc.perform(get("/api/v1/emails")
                .param("filterBy", "subject")
                .param("filterValue", "Invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Invoice"));
    }

    @Test
    void shouldReturn400WhenSendingEmailWithBlankReceiver() throws Exception {
        mockMvc.perform(post("/api/v1/send-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"\", \"subject\":\"Sub\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn400WhenSendingEmailWithBlankSubject() throws Exception {
        mockMvc.perform(post("/api/v1/send-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"user@seamail.com\", \"subject\":\"\", \"body\":\"Body\", \"priority\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

}
