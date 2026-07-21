package com.seamail.notification.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seamail.notification.event.EmailSentEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MySQL (Flyway) + real Kafka: the same event delivered twice yields exactly one
// notification (idempotent consumer), which then drives the feed, unread count,
// mark-read, and the foreign-owner 404.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class NotificationFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void consumesEventIdempotentlyAndExposesFeed() throws Exception {
        String receiver = "receiver@seamail.com";
        EmailSentEvent event = new EmailSentEvent(
                UUID.randomUUID().toString(), "EMAIL_SENT", Instant.now().toString(),
                42L, "sender@seamail.com", receiver, "Hello from Kafka");

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        KafkaTemplate<String, EmailSentEvent> template = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(),
                        new JsonSerializer<>()));

        // at-least-once delivery means the same event can arrive twice
        template.send("email.sent", receiver, event).get(10, TimeUnit.SECONDS);
        template.send("email.sent", receiver, event).get(10, TimeUnit.SECONDS);

        long notificationId = awaitSingleNotification(receiver);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // a foreign user gets a 404, not a 403 (no existence leak)
        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                .with(jwt().jwt(j -> j.subject("intruder@seamail.com"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // Polls the feed until the consumer has processed the event (or fails after 15s).
    private long awaitSingleNotification(String receiver) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                    .with(jwt().jwt(j -> j.subject(receiver))))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("content");
            if (content.size() == 1) {
                return content.get(0).get("id").asLong();
            }
            Thread.sleep(500);
        }
        fail("Notification feed did not contain exactly one entry within 15s");
        return -1; // unreachable
    }
}
