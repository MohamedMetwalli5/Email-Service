package com.seamail.notification.repository;

import com.seamail.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    private Notification sample(String eventId, String recipient) {
        return new Notification(eventId, recipient, "EMAIL_SENT", 42L,
                "Hello", "sender@seamail.com", LocalDateTime.now());
    }

    @Test
    void shouldEnforceUniqueEventId() {
        repository.saveAndFlush(sample("evt-dup", "receiver@seamail.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(sample("evt-dup", "receiver@seamail.com")));
    }

    @Test
    void shouldCountUnreadPerRecipient() {
        repository.save(sample("evt-a", "receiver@seamail.com"));
        Notification read = sample("evt-b", "receiver@seamail.com");
        read.setRead(true);
        repository.save(read);
        repository.save(sample("evt-c", "other@seamail.com"));
        repository.flush();

        assertEquals(1L, repository.countByRecipientEmailAndReadFalse("receiver@seamail.com"));
    }

    @Test
    void shouldPageRecipientFeed() {
        repository.save(sample("evt-a", "receiver@seamail.com"));
        repository.save(sample("evt-b", "receiver@seamail.com"));
        repository.flush();

        assertEquals(2, repository
                .findByRecipientEmailOrderByCreatedAtDesc("receiver@seamail.com", Pageable.unpaged())
                .getContent().size());
    }
}