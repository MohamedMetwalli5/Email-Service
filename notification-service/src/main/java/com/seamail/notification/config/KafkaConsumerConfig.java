package com.seamail.notification.config;

import com.seamail.notification.event.EmailSentEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    // After the backoff budget is exhausted, the record is published to email.sent.DLT
    // instead of blocking the partition forever (poison-pill handling).
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(5000L); // ~3 retries: 1s, 2s, 4s
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailSentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, EmailSentEvent> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EmailSentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true); // Kafka spans in traces
        return factory;
    }
}