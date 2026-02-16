package com.messenger.security;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        String secret = "your-256-bit-secret-key-here-must-be-at-least-32-characters";
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", 86400000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationInMs", 604800000L);
    }

    @Test
    void testGenerateToken() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    void testValidateToken() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void testIsTokenExpired() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testExtractClaim() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testGenerateTokenForDevice() {
        String token = jwtTokenProvider.generateTokenForDevice("testuser", "device123");
        assertNotNull(token);
        String deviceId = jwtTokenProvider.extractDeviceId(token);
        assertEquals("device123", deviceId);
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser");
        assertNotNull(token);
    }

    @Test
    void testIsTokenValid() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
        assertTrue(jwtTokenProvider.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValidWithDifferentUser() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        UserDetails userDetails = User.withUsername("differentuser")
                .password("password")
                .roles("USER")
                .build();
        assertFalse(jwtTokenProvider.isTokenValid(token, userDetails));
    }
}
