package com.messenger.integration;

import com.messenger.dto.AuthDTOs;
import com.messenger.dto.CreateDeviceRequest;
import com.messenger.dto.DeviceDTOs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class DeviceManagementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("messenger_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @Test
    void testFullDeviceLifecycle() throws Exception {
        // 1. Register a user
        AuthDTOs.RegisterRequestDTO registerRequest = AuthDTOs.RegisterRequestDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. Login
        AuthDTOs.LoginRequestDTO loginRequest = AuthDTOs.LoginRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .deviceId("test-device-123")
                .deviceName("Test Device")
                .deviceType("WEB")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthDTOs.AuthResponseDTO authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthDTOs.AuthResponseDTO.class
        );

        authToken = authResponse.getAccessToken();
        assertNotNull(authToken);

        // 3. Get devices (should have one from login)
        mockMvc.perform(get("/api/devices")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices").isArray())
                .andExpect(jsonPath("$.devices.length()").value(1));

        // 4. Get QR code for device
        mockMvc.perform(get("/api/devices/test-device-123/qr-code")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.deviceId").value("test-device-123"));
    }

    @Test
    void testDeviceValidation() throws Exception {
        // Try to create device with invalid data
        CreateDeviceRequest invalidRequest = CreateDeviceRequest.builder()
                .deviceId("")  // Empty deviceId - should fail validation
                .deviceName("Test")
                .deviceType("WEB")
                .build();

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized()); // Should be 401 since no auth token
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Try to access devices without authentication
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isUnauthorized());
    }
}
