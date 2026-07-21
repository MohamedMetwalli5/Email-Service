package com.seamail.mail.integration;

import com.seamail.mail.client.AuthUserClient;
import com.seamail.mail.event.EmailSentEvent;
import com.seamail.mail.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Full mail flow against the real security config (resource server) with the
// jwt() post-processor standing in for a token; the receiver check is mocked
// at the Feign boundary. Replaced by Testcontainers MailFlowIT in Task 12.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @MockBean
    private AuthUserClient authUserClient;

    @MockBean
    private KafkaTemplate<String, EmailSentEvent> kafkaTemplate;

    @BeforeEach
    public void setup() {
        emailRepository.deleteAll();
        org.mockito.Mockito.lenient()
                .when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(EmailSentEvent.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    @Test
    public void testUserSendEmailAndReceiverChecksInbox() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        mockMvc.perform(post("/api/v1/send-email")
                .with(jwt().jwt(j -> j.subject(senderEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"receiver\":\"" + receiverEmail + "\"," +
                        "\"subject\":\"Integration Test\"," +
                        "\"body\":\"Hello from the full flow test!\"," +
                        "\"priority\":\"1\"" +
                        "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender").value(senderEmail))
                .andExpect(jsonPath("$.content[0].subject").value("Integration Test"));
    }
}