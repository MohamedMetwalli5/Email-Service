package com.seamail.notification.service;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.entity.Notification;
import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // Idempotent consumer: Kafka delivers at-least-once, so duplicates are expected.
    // First line of defense is the existsByEventId check; the unique constraint on
    // event_id closes the race between two concurrent deliveries of the same event.
    @Transactional
    public void recordEmailSent(EmailSentEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            log.info("Duplicate event {} - already recorded, skipping", event.eventId());
            return;
        }
        try {
            repository.save(new Notification(
                    event.eventId(),
                    event.receiver(),
                    "EMAIL_SENT",
                    event.emailId(),
                    event.subject(),
                    event.sender(),
                    LocalDateTime.now()
            ));
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate event {} detected via unique constraint - skipping", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(String recipientEmail, Pageable pageable) {
        return repository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientEmail) {
        return repository.countByRecipientEmailAndReadFalse(recipientEmail);
    }

    @Transactional
    public void markAsRead(Long id, String recipientEmail) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
        if (!notification.getRecipientEmail().equals(recipientEmail)) {
            // 404 (not 403) on purpose: do not leak the existence of other users' notifications
            throw new NotificationNotFoundException("Notification not found: " + id);
        }
        notification.setRead(true);
        repository.save(notification);
    }

    private NotificationResponseDto toDto(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getSourceEmailId(),
                n.getSubjectSnapshot(),
                n.getSenderSnapshot(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}