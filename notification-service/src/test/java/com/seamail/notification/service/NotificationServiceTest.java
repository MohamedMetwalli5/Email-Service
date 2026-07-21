package com.seamail.notification.service;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.entity.Notification;
import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService notificationService;

    private EmailSentEvent sampleEvent() {
        return new EmailSentEvent("evt-1", "EMAIL_SENT", "2026-07-21T10:00:00Z",
                42L, "sender@seamail.com", "receiver@seamail.com", "Hello");
    }

    @Test
    void shouldRecordNotificationForNewEvent() {
        when(repository.existsByEventId("evt-1")).thenReturn(false);

        notificationService.recordEmailSent(sampleEvent());

        verify(repository).save(any(Notification.class));
    }

    @Test
    void shouldSkipDuplicateEvent() {
        when(repository.existsByEventId("evt-1")).thenReturn(true);

        notificationService.recordEmailSent(sampleEvent());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldSkipWhenUniqueConstraintFiresOnRace() {
        when(repository.existsByEventId("evt-1")).thenReturn(false);
        when(repository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        assertDoesNotThrow(() -> notificationService.recordEmailSent(sampleEvent()));
    }

    @Test
    void shouldReturnUnreadCount() {
        when(repository.countByRecipientEmailAndReadFalse("receiver@seamail.com")).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount("receiver@seamail.com"));
    }

    @Test
    void shouldReturnPaginatedFeed() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findByRecipientEmailOrderByCreatedAtDesc(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponseDto> page =
                notificationService.getNotifications("receiver@seamail.com", Pageable.unpaged());

        assertEquals(1, page.getContent().size());
        assertEquals("Hello", page.getContent().get(0).subjectSnapshot());
    }

    @Test
    void shouldMarkAsReadWhenOwner() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, "receiver@seamail.com");

        assertTrue(n.isRead());
        verify(repository).save(n);
    }

    @Test
    void shouldThrow404WhenMarkingForeignNotification() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(1L, "other@seamail.com"));
    }

    @Test
    void shouldThrow404WhenNotificationMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(99L, "receiver@seamail.com"));
    }
}