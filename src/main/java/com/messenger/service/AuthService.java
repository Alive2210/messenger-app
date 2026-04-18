package com.messenger.service;

import com.messenger.dto.AuthDTOs.*;
import com.messenger.encryption.EncryptionService;
import com.messenger.entity.User;
import com.messenger.logging.Auditable;
import com.messenger.logging.MessengerLogger;
import com.messenger.repository.DeviceRepository;
import com.messenger.repository.UserRepository;
import com.messenger.security.JwtTokenProvider;
import com.messenger.security.PBKDF2Service;
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
import java.util.HashMap;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final PBKDF2Service pbkdf2Service;

    @Transactional
    @Auditable(action = "USER_REGISTRATION")
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.debug("Starting user registration for: {}", request.getUsername());

        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            MessengerLogger.securityAuthFailure(request.getUsername(), "USERNAME_EXISTS", "unknown");
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            MessengerLogger.securityAuthFailure(request.getEmail(), "EMAIL_EXISTS", "unknown");
            throw new IllegalArgumentException("Email already registered");
        }

        // Generate RSA key pair for E2E encryption if not provided
        String publicKey = request.getPublicKey();
        String encryptedPrivateKey = null;
        String salt = null;

        if (publicKey == null || publicKey.isEmpty()) {
            KeyPair keyPair = encryptionService.generateRSAKeyPair();
            publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            
            try {
                String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
                
                // SECURE: Generate random salt and derive AES key using PBKDF2
                salt = pbkdf2Service.generateSalt();
                String aesKey = pbkdf2Service.deriveKeyFromPassword(request.getPassword(), salt);
                encryptedPrivateKey = encryptionService.encryptMessage(privateKey, aesKey);
                
                log.info("Private key encrypted with PBKDF2-derived AES key for user: {}", request.getUsername());
            } catch (Exception e) {
                log.error("Failed to encrypt private key, registration will fail", e);
                throw new RuntimeException("Failed to encrypt private key: " + e.getMessage(), e);
            }
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .publicKey(publicKey)
                .privateKeyEncrypted(encryptedPrivateKey)
                .passwordSalt(salt) // Store salt for key derivation
                .phoneNumber(request.getPhoneNumber())
                .isOnline(false)
                .build();

        // Flush immediately so the authentication step can see the new user
        // within the same request/transaction.
        userRepository.saveAndFlush(user);

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
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Use the authenticated principal name (may differ from the identifier the user typed, e.g. email).
            String authenticatedUsername = authentication.getName();

            // Generate token with device ID if provided
            String accessToken;
            if (deviceId != null && !deviceId.isEmpty()) {
                accessToken = jwtTokenProvider.generateTokenForDevice(authenticatedUsername, deviceId);
            } else {
                accessToken = jwtTokenProvider.generateToken(authentication);
            }
            String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUsername);

            User user = userRepository.findByUsername(authenticatedUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Update online status
            user.setIsOnline(true);
            userRepository.save(user);

            // Register device if device info provided
            if (deviceId != null && !deviceId.isEmpty()) {
                registerDevice(user, deviceId, deviceName, deviceType, osVersion, appVersion);
            }

            log.info("User {} logged in successfully (device: {})", authenticatedUsername, deviceId);

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L) // 24 hours
                    .user(mapToUserDTO(user))
                    .build();

        } catch (BadCredentialsException e) {
            MessengerLogger.securityAuthFailure(username, "INVALID_CREDENTIALS", deviceId);
            throw e;
        }
    }

    private void registerDevice(User user, String deviceId, String deviceName,
            String deviceType, String osVersion, String appVersion) {
        try {
            com.messenger.entity.Device.DeviceType type = com.messenger.entity.Device.DeviceType.UNKNOWN;
            if (deviceType != null) {
                try {
                    type = com.messenger.entity.Device.DeviceType.valueOf(deviceType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown device type: {}", deviceType);
                }
            }

            // Create or update device
            com.messenger.entity.Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElse(com.messenger.entity.Device.builder().deviceId(deviceId).build());

            device.setUser(user);
            device.setDeviceName(deviceName != null ? deviceName : "Unknown Device");
            device.setDeviceType(type);
            device.setOsVersion(osVersion);
            device.setAppVersion(appVersion);
            device.setIsOnline(true);
            device.setLastSeen(java.time.LocalDateTime.now());
            device.setLastHeartbeat(java.time.LocalDateTime.now());
            device.setIsActive(true);

            deviceRepository.save(device);
            log.debug("Device registered and saved: {} for user {}", deviceId, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to register device: {}", e.getMessage(), e);
        }
    }

    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // JwtTokenProvider.generateToken(Authentication) expects a UserDetails principal; generate token directly.
        String newAccessToken = jwtTokenProvider.generateToken(new HashMap<>(), user.getUsername());
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
        if (token == null || token.isEmpty()) {
            log.warn("Logout called with null or empty token");
            return;
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Logout called with invalid token");
                return;
            }
            String username = jwtTokenProvider.getUsernameFromToken(token);
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setIsOnline(false);
                userRepository.save(user);
                log.info("User logged out: {}", username);
            });
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
        }
    }

    public PublicKeyDTO getPublicKey(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
