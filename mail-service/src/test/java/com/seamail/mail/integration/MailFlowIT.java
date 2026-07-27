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
import org.springframework.test.web.servlet.MvcResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MySQL (Flyway) + real Kafka. Exercises the full mailbox lifecycle through the
// resource-server filter chain: send -> inbox (receiver) + outbox (sender) ->
// move-to-trash -> trashbox -> permanent delete, and asserts exactly one EmailSentEvent
// was published to email.sent (only send emits; trash/delete are silent). The Feign
// receiver check is mocked at the boundary; the other side of the Kafka contract is
// covered by NotificationFlowIT.
//
// Auth is exercised by the OAuth2 resource-server filter chain via the jwt() MockMvc
// post-processor. Real JWKS validation against a live auth-service is intentionally not
// wired here: doing so would couple two services' container lifecycle into one IT and
// duplicate what controller-level slice tests already prove (resource-server 401 on
// missing/invalid tokens); the slice tests already cover that filter behavior.
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
    void sendAndMailboxLifecyclePersistsAndPublishesExactlyOneEvent() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        // send -> 201
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

        // receiver's inbox has the new email; capture its id for the lifecycle steps
        MvcResult inboxResult = mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender").value(senderEmail))
                .andExpect(jsonPath("$.content[0].subject").value("Integration Test"))
                .andReturn();
        long emailId = extractEmailId(inboxResult);
        assertTrue(emailId > 0, "Sent email should have a positive id");

        // sender's outbox reflects the same sent email
        mockMvc.perform(get("/api/v1/outbox")
                .with(jwt().jwt(j -> j.subject(senderEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].receiver").value(receiverEmail));

        // receiver moves the email to trash; inbox becomes empty, trashbox has one
        mockMvc.perform(post("/api/v1/move-to-trash")
                .with(jwt().jwt(j -> j.subject(receiverEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":" + emailId + "}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/trashbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].emailID").value(emailId));

        // permanent delete is only allowed after move-to-trash; trashbox becomes empty
        mockMvc.perform(delete("/api/v1/delete-email")
                .with(jwt().jwt(j -> j.subject(receiverEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailId\":" + emailId + "}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/trashbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // exactly one EmailSentEvent hit the topic: trash and delete publish nothing
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
            assertEquals(emailId, event.emailId());
        }
    }

    private long extractEmailId(MvcResult result) throws Exception {
        com.fasterxml.jackson.databind.JsonNode content =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(result.getResponse().getContentAsString())
                        .get("content");
        if (content == null || content.size() == 0) {
            return -1;
        }
        return content.get(0).get("emailID").asLong();
    }
}