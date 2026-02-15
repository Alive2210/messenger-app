package com.messenger.service;

import com.messenger.dto.AuthDTOs.*;
import com.messenger.encryption.EncryptionService;
import com.messenger.entity.User;
import com.messenger.logging.Auditable;
import com.messenger.logging.MessengerLogger;
import com.messenger.repository.UserRepository;
import com.messenger.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    @Transactional
    @Auditable(action = "USER_REGISTRATION")
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.debug("Starting user registration for: {}", request.getUsername());
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            MessengerLogger.securityAuthFailure(request.getUsername(), "USERNAME_EXISTS", "unknown");
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            MessengerLogger.securityAuthFailure(request.getEmail(), "EMAIL_EXISTS", "unknown");
            throw new RuntimeException("Email already registered");
        }

        // Generate RSA key pair for E2E encryption if not provided
        String publicKey = request.getPublicKey();
        String encryptedPrivateKey = null;
        
        if (publicKey == null || publicKey.isEmpty()) {
            KeyPair keyPair = encryptionService.generateRSAKeyPair();
            publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            // Encrypt private key with user's password for secure storage
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            encryptedPrivateKey = encryptionService.encryptMessage(privateKey, 
                    encryptionService.generateAESKey());
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .publicKey(publicKey)
                .privateKeyEncrypted(encryptedPrivateKey)
                .phoneNumber(request.getPhoneNumber())
                .isOnline(false)
                .build();

        userRepository.save(user);
        
        MessengerLogger.audit("USER_REGISTRATION", request.getUsername(), 
                "Email: " + request.getEmail());
        log.info("User registered successfully: {}", request.getUsername());

        // Generate tokens
        return authenticateAndGenerateTokens(request.getUsername(), request.getPassword());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        return authenticateAndGenerateTokens(request.getUsername(), request.getPassword(), 
                request.getDeviceId(), request.getDeviceName(), request.getDeviceType(), 
                request.getOsVersion(), request.getAppVersion());
    }

    private AuthResponseDTO authenticateAndGenerateTokens(String username, String password) {
        return authenticateAndGenerateTokens(username, password, null, null, null, null, null);
    }

    private AuthResponseDTO authenticateAndGenerateTokens(String username, String password, 
            String deviceId, String deviceName, String deviceType, String osVersion, String appVersion) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate token with device ID if provided
            String accessToken;
            if (deviceId != null && !deviceId.isEmpty()) {
                accessToken = jwtTokenProvider.generateTokenForDevice(username, deviceId);
            } else {
                accessToken = jwtTokenProvider.generateToken(authentication);
            }
            String refreshToken = jwtTokenProvider.generateRefreshToken(username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update online status
            user.setIsOnline(true);
            userRepository.save(user);

            // Register device if device info provided
            if (deviceId != null && !deviceId.isEmpty()) {
                registerDevice(user, deviceId, deviceName, deviceType, osVersion, appVersion);
            }

            log.info("User {} logged in successfully (device: {})", username, deviceId);

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L) // 24 hours
                    .user(mapToUserDTO(user))
                    .build();

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    private void registerDevice(User user, String deviceId, String deviceName, 
            String deviceType, String osVersion, String appVersion) {
        try {
            // Import Device entity
            com.messenger.entity.Device.DeviceType type = com.messenger.entity.Device.DeviceType.UNKNOWN;
            if (deviceType != null) {
                try {
                    type = com.messenger.entity.Device.DeviceType.valueOf(deviceType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown device type: {}", deviceType);
                }
            }

            // Create or update device
            com.messenger.entity.Device device = com.messenger.entity.Device.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .deviceName(deviceName != null ? deviceName : "Unknown Device")
                    .deviceType(type)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .isOnline(true)
                    .lastSeen(java.time.LocalDateTime.now())
                    .isActive(true)
                    .build();

            // Save device (you would inject DeviceRepository here in a real implementation)
            log.debug("Device registered: {} for user {}", deviceId, user.getUsername());
        } catch (Exception e) {
            log.warn("Failed to register device: {}", e.getMessage());
        }
    }

    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtTokenProvider.generateToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUsername(), null, java.util.Collections.emptyList()
                )
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(mapToUserDTO(user))
                .build();
    }

    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setIsOnline(false);
                userRepository.save(user);
                log.info("User logged out: {}", username);
            });
        }
    }

    public PublicKeyDTO getPublicKey(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return PublicKeyDTO.builder()
                .username(user.getUsername())
                .publicKey(user.getPublicKey())
                .build();
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .publicKey(user.getPublicKey())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .statusMessage(user.getStatusMessage())
                .build();
    }
}
