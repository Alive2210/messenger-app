package com.messenger.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", 
                "dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgdGVzdGluZyE=");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 86400000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationInMs", 604800000L);
    }

    @Test
    @DisplayName("Should generate valid token")
    void shouldGenerateValidToken() {
        Authentication auth = createAuthentication("testuser");
        
        String token = tokenProvider.generateToken(auth);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        Authentication auth = createAuthentication("testuser");
        String token = tokenProvider.generateToken(auth);
        
        boolean isValid = tokenProvider.validateToken(token);
        
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        Authentication auth = createAuthentication("testuser");
        String token = tokenProvider.generateToken(auth);
        
        String username = tokenProvider.getUsernameFromToken(token);
        
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        boolean isValid = tokenProvider.validateToken("invalid.token.here");
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        boolean isValid = tokenProvider.validateToken("");
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        boolean isValid = tokenProvider.validateToken("not.a.valid.token");
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate refresh token")
    void shouldGenerateRefreshToken() {
        String token = tokenProvider.generateRefreshToken("testuser");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should validate refresh token")
    void shouldValidateRefreshToken() {
        String refreshToken = tokenProvider.generateRefreshToken("testuser");
        
        boolean isValid = tokenProvider.validateToken(refreshToken);
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        
        assertTrue(isValid);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        Authentication auth1 = createAuthentication("user1");
        Authentication auth2 = createAuthentication("user2");
        
        String token1 = tokenProvider.generateToken(auth1);
        String token2 = tokenProvider.generateToken(auth2);
        
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should validate token against user details")
    void shouldValidateTokenAgainstUserDetails() {
        Authentication auth = createAuthentication("testuser");
        String token = tokenProvider.generateToken(auth);
        
        boolean isValid = tokenProvider.isTokenValid(token, 
                new org.springframework.security.core.userdetails.User(
                        "testuser", "password", Collections.emptyList()));
        
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        Authentication auth = createAuthentication("user1");
        String token = tokenProvider.generateToken(auth);
        
        boolean isValid = tokenProvider.isTokenValid(token,
                new org.springframework.security.core.userdetails.User(
                        "user2", "password", Collections.emptyList()));
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract claim from token")
    void shouldExtractClaimFromToken() {
        Authentication auth = createAuthentication("testuser");
        String token = tokenProvider.generateToken(auth);
        
        String subject = tokenProvider.extractClaim(token, 
                claims -> claims.getSubject());
        
        assertEquals("testuser", subject);
    }

    @Test
    @DisplayName("Should generate token with device ID")
    void shouldGenerateTokenWithDeviceId() {
        String deviceId = "device-123-abc";
        
        String token = tokenProvider.generateTokenForDevice("testuser", deviceId);
        
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        
        String extractedDeviceId = tokenProvider.extractDeviceId(token);
        assertEquals(deviceId, extractedDeviceId);
    }

    @Test
    @DisplayName("Should extract device ID from token")
    void shouldExtractDeviceIdFromToken() {
        String deviceId = "my-device-id";
        String token = tokenProvider.generateTokenForDevice("testuser", deviceId);
        
        String extractedDeviceId = tokenProvider.extractDeviceId(token);
        
        assertEquals(deviceId, extractedDeviceId);
    }

    @Test
    @DisplayName("Should return null device ID for token without device claim")
    void shouldReturnNullDeviceIdForTokenWithoutDeviceClaim() {
        Authentication auth = createAuthentication("testuser");
        String token = tokenProvider.generateToken(auth);
        
        String extractedDeviceId = tokenProvider.extractDeviceId(token);
        
        assertNull(extractedDeviceId);
    }

    @Test
    @DisplayName("Should validate token with device ID")
    void shouldValidateTokenWithDeviceId() {
        String token = tokenProvider.generateTokenForDevice("testuser", "device-123");
        
        boolean isValid = tokenProvider.validateToken(token);
        String username = tokenProvider.getUsernameFromToken(token);
        
        assertTrue(isValid);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should generate different tokens for different devices")
    void shouldGenerateDifferentTokensForDifferentDevices() {
        String token1 = tokenProvider.generateTokenForDevice("testuser", "device-1");
        String token2 = tokenProvider.generateTokenForDevice("testuser", "device-2");
        
        assertNotEquals(token1, token2);
        
        assertEquals("device-1", tokenProvider.extractDeviceId(token1));
        assertEquals("device-2", tokenProvider.extractDeviceId(token2));
    }

    private Authentication createAuthentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        username, "password", 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
