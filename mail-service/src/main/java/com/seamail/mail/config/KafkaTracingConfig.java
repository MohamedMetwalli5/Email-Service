package com.seamail.mail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

// Enables Micrometer observation on the producer so the email.sent publish
// appears as a span between mail-service and the consumer.
@Configuration
public class KafkaTracingConfig {

    public KafkaTracingConfig(KafkaTemplate<?, ?> kafkaTemplate) {
        kafkaTemplate.setObservationEnabled(true);
    }
}