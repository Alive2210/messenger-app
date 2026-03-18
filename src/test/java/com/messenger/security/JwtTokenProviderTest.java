package com.messenger.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "test-secret-key-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", 86400000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationInMs", 604800000L);
    }

    @Test
    @DisplayName("Should generate token with extra claims")
    void shouldGenerateTokenWithExtraClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-123");
        
        String token = jwtTokenProvider.generateToken(claims, "testuser");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should generate refresh token")
    void shouldGenerateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should generate token for device")
    void shouldGenerateTokenForDevice() {
        String token = jwtTokenProvider.generateTokenForDevice("testuser", "device-123");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        String deviceId = jwtTokenProvider.extractDeviceId(token);
        assertEquals("device-123", deviceId);
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateToken() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("Should extract expiration date")
    void shouldExtractExpiration() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        
        Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Should validate token with user details")
    void shouldValidateTokenWithUserDetails() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        
        UserDetails userDetails = User.withUsername("testuser").password("password").build();
        
        assertTrue(jwtTokenProvider.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should return false when username doesn't match")
    void shouldReturnFalseWhenUsernameDoesNotMatch() {
        String token = jwtTokenProvider.generateToken(new HashMap<>(), "testuser");
        
        UserDetails userDetails = User.withUsername("differentuser").password("password").build();
        
        assertFalse(jwtTokenProvider.isTokenValid(token, userDetails));
    }
}
