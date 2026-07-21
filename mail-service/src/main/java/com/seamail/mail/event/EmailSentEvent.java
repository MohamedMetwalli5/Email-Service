package com.seamail.mail.event;

// Contract for the email.sent Kafka topic.
// occurredAt is an ISO-8601 string to keep JSON serialization trivial and explicit.
public record EmailSentEvent(
        String eventId,
        String eventType,
        String occurredAt,
        Long emailId,
        String sender,
        String receiver,
        String subject
) {}