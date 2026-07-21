package com.seamail.notification.messaging;

import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailSentConsumer {

    private final NotificationService notificationService;

    public EmailSentConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${kafka.topic.email-sent:email.sent}", groupId = "notification-service")
    public void onEmailSent(EmailSentEvent event) {
        notificationService.recordEmailSent(event);
    }
}