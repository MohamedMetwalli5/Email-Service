package com.seamail.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// End-to-end auth flow against real MySQL (Flyway migrations applied) and real Redis:
// sign-up -> sign-in -> authenticated call with the RS256 token -> refresh rotation
// -> old refresh token rejected -> JWKS endpoint exposes the public key.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void signupSigninRefreshRotationAndJwks() throws Exception {
        String email = "flow@seamail.com";
        String password = "pass1234";
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

        // sign-up -> 201 with a token pair
        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // sign-in -> 200 with a token pair
        MvcResult login = mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokens = objectMapper.readTree(login.getResponse().getContentAsString());
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        // the RS256 access token authenticates a protected call (local decoder path)
        mockMvc.perform(put("/api/v1/update-language")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"language\":\"de\"}"))
                .andExpect(status().isNoContent());

        // refresh rotates: old token is consumed, a new pair is issued
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rotated = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        assertNotEquals(refreshToken, rotated.get("refreshToken").asText());

        // the consumed refresh token is rejected on reuse
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        // JWKS endpoint publishes the RSA public key
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").exists());
    }
}
