package com.backendemailservice.backendemailservice;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.EmailRepository;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    public void setup() {
        emailRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testUserSendEmailAndReceiverChecksInbox() throws Exception {
        // 1. Creating two real users in the database
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        userRepository.save(new User(senderEmail, passwordEncoder.encode("pass123")));
        userRepository.save(new User(receiverEmail, passwordEncoder.encode("pass456")));

        String senderToken = jwtUtil.generateToken(senderEmail);
        String receiverToken = jwtUtil.generateToken(receiverEmail);

        // 2. Sending an email via the API
        mockMvc.perform(post("/api/v1/send-email")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"receiver\":\"" + receiverEmail + "\"," +
                        "\"subject\":\"Integration Test\"," +
                        "\"body\":\"Hello from the full flow test!\"," +
                        "\"priority\":\"1\"" +
                        "}"))
                .andExpect(status().isOk());

        // 3. Receiver checks inbox via the API
        mockMvc.perform(get("/api/v1/inbox")
                .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sender").value(senderEmail))
                .andExpect(jsonPath("$[0].subject").value("Integration Test"));
    }
}