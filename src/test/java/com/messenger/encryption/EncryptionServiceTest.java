package com.messenger.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    @DisplayName("Should generate AES key")
    void shouldGenerateAESKey() {
        String key = encryptionService.generateAESKey();

        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    @DisplayName("Should generate different AES keys")
    void shouldGenerateDifferentAESKeys() {
        String key1 = encryptionService.generateAESKey();
        String key2 = encryptionService.generateAESKey();

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("Should generate RSA key pair")
    void shouldGenerateRSAKeyPair() {
        KeyPair keyPair = encryptionService.generateRSAKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    @DisplayName("Should encrypt and decrypt message")
    void shouldEncryptAndDecryptMessage() {
        String originalMessage = "Hello, World! This is a test message.";
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        String decrypted = encryptionService.decryptMessage(encrypted, aesKey);

        assertNotEquals(originalMessage, encrypted);
        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt with special characters")
    void shouldEncryptAndDecryptWithSpecialCharacters() {
        String originalMessage = "Hello! @#$%^&*()_+{}[]|;':\",./<>?";
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        String decrypted = encryptionService.decryptMessage(encrypted, aesKey);

        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt Unicode text")
    void shouldEncryptAndDecryptUnicodeText() {
        String originalMessage = "Привет мир! こんにちは 🎉 Émojis: 👍🚀💻";
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        String decrypted = encryptionService.decryptMessage(encrypted, aesKey);

        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt empty string")
    void shouldEncryptAndDecryptEmptyString() {
        String originalMessage = "";
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        String decrypted = encryptionService.decryptMessage(encrypted, aesKey);

        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt long message")
    void shouldEncryptAndDecryptLongMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a long test message. ");
        }
        String originalMessage = sb.toString();
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        String decrypted = encryptionService.decryptMessage(encrypted, aesKey);

        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Should encrypt AES key with RSA")
    void shouldEncryptAESKeyWithRSA() {
        KeyPair keyPair = encryptionService.generateRSAKeyPair();
        String aesKey = encryptionService.generateAESKey();

        String publicKey = java.util.Base64.getEncoder().encodeToString(
                keyPair.getPublic().getEncoded());
        String privateKey = java.util.Base64.getEncoder().encodeToString(
                keyPair.getPrivate().getEncoded());

        String encryptedKey = encryptionService.encryptKeyWithRSA(aesKey, publicKey);
        String decryptedKey = encryptionService.decryptKeyWithRSA(encryptedKey, privateKey);

        assertNotEquals(aesKey, encryptedKey);
        assertEquals(aesKey, decryptedKey);
    }

    @Test
    @DisplayName("Should generate and verify salt")
    void shouldGenerateAndVerifySalt() {
        String salt1 = encryptionService.generateSalt();
        String salt2 = encryptionService.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2);
        assertEquals(24, salt1.length()); // Base64 of 16 bytes
    }

    @Test
    @DisplayName("Should hash password with PBKDF2")
    void shouldHashPasswordWithPBKDF2() {
        String password = "securePassword123!";
        String salt = encryptionService.generateSalt();

        String hash1 = encryptionService.hashPassword(password, salt);
        String hash2 = encryptionService.hashPassword(password, salt);

        assertNotNull(hash1);
        assertEquals(hash1, hash2); // Same password + salt = same hash
    }

    @Test
    @DisplayName("Should generate different hashes for different passwords")
    void shouldGenerateDifferentHashesForDifferentPasswords() {
        String salt = encryptionService.generateSalt();
        String password1 = "password1";
        String password2 = "password2";

        String hash1 = encryptionService.hashPassword(password1, salt);
        String hash2 = encryptionService.hashPassword(password2, salt);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should generate secure token")
    void shouldGenerateSecureToken() {
        String token1 = encryptionService.generateSecureToken();
        String token2 = encryptionService.generateSecureToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() > 20);
    }

    @Test
    @DisplayName("Should throw exception on wrong key decryption")
    void shouldThrowExceptionOnWrongKeyDecryption() {
        String originalMessage = "Test message";
        String aesKey1 = encryptionService.generateAESKey();
        String aesKey2 = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey1);

        assertThrows(RuntimeException.class, () -> {
            encryptionService.decryptMessage(encrypted, aesKey2);
        });
    }

    @Test
    @DisplayName("Should handle tampered encrypted data")
    void shouldHandleTamperedEncryptedData() {
        String originalMessage = "Test message";
        String aesKey = encryptionService.generateAESKey();

        String encrypted = encryptionService.encryptMessage(originalMessage, aesKey);
        
        // Tamper with the encrypted data
        String tampered = encrypted.substring(0, encrypted.length() - 10) + "TAMPERED!!";

        assertThrows(RuntimeException.class, () -> {
            encryptionService.decryptMessage(tampered, aesKey);
        });
    }
}
