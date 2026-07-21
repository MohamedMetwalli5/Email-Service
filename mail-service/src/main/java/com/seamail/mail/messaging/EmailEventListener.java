package com.seamail.mail.messaging;

import com.seamail.mail.event.EmailSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Publishes domain events only after the DB transaction commits, so consumers
// never see events for rolled-back emails. Deliberately not a full outbox table:
// a broker hiccup between commit and send can drop an event (at-most-once) -
// the trade-off is documented in the README talking points.
@Component
public class EmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailEventListener.class);

    private final KafkaTemplate<String, EmailSentEvent> kafkaTemplate;
    private final String topic;

    public EmailEventListener(KafkaTemplate<String, EmailSentEvent> kafkaTemplate,
                              @Value("${kafka.topic.email-sent:email.sent}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailSent(EmailSentEvent event) {
        kafkaTemplate.send(topic, event.receiver(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to {}", event.eventId(), topic, ex);
                    } else {
                        log.info("Published event {} to {}", event.eventId(), topic);
                    }
                });
    }
}