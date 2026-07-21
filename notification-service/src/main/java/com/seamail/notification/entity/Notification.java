package com.seamail.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "source_email_id", nullable = false)
    private Long sourceEmailId;

    @Column(name = "subject_snapshot", nullable = false)
    private String subjectSnapshot;

    @Column(name = "sender_snapshot", nullable = false)
    private String senderSnapshot;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(String eventId, String recipientEmail, String type, Long sourceEmailId,
                        String subjectSnapshot, String senderSnapshot, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.sourceEmailId = sourceEmailId;
        this.subjectSnapshot = subjectSnapshot;
        this.senderSnapshot = senderSnapshot;
        this.read = false;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getType() {
        return type;
    }

    public Long getSourceEmailId() {
        return sourceEmailId;
    }

    public String getSubjectSnapshot() {
        return subjectSnapshot;
    }

    public String getSenderSnapshot() {
        return senderSnapshot;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}