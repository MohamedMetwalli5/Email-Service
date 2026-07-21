package com.seamail.notification.repository;

import com.seamail.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    long countByRecipientEmailAndReadFalse(String recipientEmail);

    boolean existsByEventId(String eventId);
}