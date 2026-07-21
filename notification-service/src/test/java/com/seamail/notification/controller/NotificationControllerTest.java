package com.seamail.notification.controller;

import com.seamail.notification.config.TestSecurityConfig;
import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    private static final String TEST_EMAIL = "receiver@seamail.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void shouldReturnPaginatedFeed() throws Exception {
        NotificationResponseDto dto = new NotificationResponseDto(1L, "EMAIL_SENT", 42L,
                "Hello", "sender@seamail.com", false, LocalDateTime.now());
        when(notificationService.getNotifications(eq(TEST_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subjectSnapshot").value("Hello"));
    }

    @Test
    void shouldReturnUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(TEST_EMAIL)).thenReturn(2L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void shouldReturn204WhenMarkingRead() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/1/read")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(1L, TEST_EMAIL);
    }

    @Test
    void shouldReturn404WhenNotificationNotFound() throws Exception {
        doThrow(new NotificationNotFoundException("Notification not found: 9"))
                .when(notificationService).markAsRead(9L, TEST_EMAIL);

        mockMvc.perform(post("/api/v1/notifications/9/read")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_NOT_FOUND"));
    }
}