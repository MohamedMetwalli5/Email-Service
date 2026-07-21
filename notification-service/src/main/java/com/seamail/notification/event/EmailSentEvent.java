package com.seamail.notification.event;

// Consumer-side view of the email.sent contract (mail-service's event record).
// Duplicated intentionally: services share contracts, not code.
public record EmailSentEvent(
        String eventId,
        String eventType,
        String occurredAt,
        Long emailId,
        String sender,
        String receiver,
        String subject
) {}