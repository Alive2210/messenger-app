package com.messenger.controller;

import com.messenger.dto.AuthDTOs.*;
import com.messenger.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.messenger.exception.GlobalExceptionHandler.ErrorResponse;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Registration attempt for user: {}", request.getUsername());
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @RateLimiter(name = "login", fallbackMethod = "loginFallback")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login attempt for user: {}", request.getUsername());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ErrorResponse> loginFallback(LoginRequestDTO request, Throwable ex) {
        log.warn("Rate limit exceeded for user: {}. Error: {}", request.getUsername(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Please try again later",
                "/api/auth/login");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public-key/{username}")
    public ResponseEntity<PublicKeyDTO> getPublicKey(@PathVariable String username) {
        PublicKeyDTO publicKey = authService.getPublicKey(username);
        return ResponseEntity.ok(publicKey);
    }
}
