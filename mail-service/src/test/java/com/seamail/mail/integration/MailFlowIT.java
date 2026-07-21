package com.seamail.mail.integration;

import com.seamail.mail.client.AuthUserClient;
import com.seamail.mail.event.EmailSentEvent;
import com.seamail.mail.repository.EmailRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MySQL (Flyway) + real Kafka: send an email through the API, verify inbox delivery
// and exactly one EmailSentEvent on the topic. The Feign receiver check is mocked at the
// boundary; the other side of the Kafka contract is covered by NotificationFlowIT.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class MailFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @MockBean
    private AuthUserClient authUserClient;

    @BeforeEach
    void setup() {
        emailRepository.deleteAll();
    }

    @Test
    void sendEmailPersistsAndPublishesExactlyOneEvent() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        mockMvc.perform(post("/api/v1/send-email")
                .with(jwt().jwt(j -> j.subject(senderEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"receiver\":\"" + receiverEmail + "\"," +
                        "\"subject\":\"Integration Test\"," +
                        "\"body\":\"Hello from MailFlowIT!\"," +
                        "\"priority\":\"1\"" +
                        "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender").value(senderEmail))
                .andExpect(jsonPath("$.content[0].subject").value("Integration Test"));

        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "mail-flow-it", "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, EmailSentEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(),
                new JsonDeserializer<>(EmailSentEvent.class, false)).createConsumer()) {
            consumer.subscribe(List.of("email.sent"));
            ConsumerRecords<String, EmailSentEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

            assertEquals(1, records.count());
            EmailSentEvent event = records.iterator().next().value();
            assertEquals("EMAIL_SENT", event.eventType());
            assertEquals(senderEmail, event.sender());
            assertEquals(receiverEmail, event.receiver());
            assertEquals("Integration Test", event.subject());
            assertNotNull(event.eventId());
            assertNotNull(event.emailId());
        }
    }
}
