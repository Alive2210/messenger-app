package com.messenger.controller;

import com.messenger.dto.AuthDTOs.*;
import com.messenger.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletResponse response) {
        log.info("Registration attempt for user: {}", request.getUsername());
        AuthResponseDTO authResponse = authService.register(request);
        setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response) {
        log.info("Login attempt for user: {}", request.getUsername());
        log.info("Login request - username: {}, deviceId: {}, deviceName: {}", 
            request.getUsername(), request.getDeviceId(), request.getDeviceName());
        AuthResponseDTO authResponse = authService.login(request);
        setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        log.info("Login successful for user: {}", request.getUsername());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @RequestBody RefreshTokenRequestDTO request,
            HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.refreshToken(request.getRefreshToken());
        setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Try to get token from Authorization header first, then from cookies
        String jwt = null;
        if (token != null && token.startsWith("Bearer ")) {
            jwt = token.replace("Bearer ", "");
        } else {
            // Fallback to cookie
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        if (jwt != null) {
            authService.logout(jwt);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public-key/{username}")
    public ResponseEntity<PublicKeyDTO> getPublicKey(@PathVariable String username) {
        PublicKeyDTO publicKey = authService.getPublicKey(username);
        return ResponseEntity.ok(publicKey);
    }

    /**
     * Set HttpOnly, Secure cookies for authentication tokens
     * 
     * Security features:
     * - HttpOnly: Prevents XSS attacks (JavaScript cannot access cookies)
     * - Secure: Only sent over HTTPS
     * - SameSite=Strict: Prevents CSRF attacks
     * - Path=/: Available to all endpoints
     */
    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token Cookie (short-lived: 24 hours)
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // HTTPS only
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(24 * 60 * 60); // 24 hours
        accessTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessTokenCookie);

        // Refresh Token Cookie (long-lived: 7 days)
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS only
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);

        log.debug("Auth cookies set successfully");
    }

    /**
     * Clear authentication cookies on logout
     */
    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // Delete immediately
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // Delete immediately
        response.addCookie(refreshTokenCookie);

        log.debug("Auth cookies cleared");
    }
}
