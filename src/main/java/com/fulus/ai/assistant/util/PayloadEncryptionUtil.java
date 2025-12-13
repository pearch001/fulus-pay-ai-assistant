package com.fulus.ai.assistant.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Utility class for encrypting/decrypting transaction payloads
 * Uses AES-256-GCM for authenticated encryption
 */
@Slf4j
public class PayloadEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Encrypt payload JSON
     *
     * @param payload Transaction payload (JSON object)
     * @param encryptionKey Encryption key (32 bytes for AES-256)
     * @return Base64 encoded encrypted payload
     */
    public static String encryptPayload(Map<String, Object> payload, String encryptionKey) {
        try {
            // Convert payload to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Generate IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    encryptionKey.getBytes(StandardCharsets.UTF_8),
                    "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Failed to encrypt payload", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt payload
     *
     * @param encryptedPayload Base64 encoded encrypted payload
     * @param encryptionKey Encryption key
     * @return Decrypted payload as JSON string
     */
    public static String decryptPayload(String encryptedPayload, String encryptionKey) {
        try {
            // Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encryptedPayload);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    encryptionKey.getBytes(StandardCharsets.UTF_8),
                    "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decrypt payload", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate encryption key (for development/testing)
     * In production, use proper key management system
     */
    public static String generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("Failed to generate encryption key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Derive encryption key from user credentials (mock)
     * In production, use proper key derivation (PBKDF2, scrypt, etc.)
     */
    public static String deriveEncryptionKey(String phoneNumber, String pin) {
        String combined = phoneNumber + ":" + pin;
        String hash = TransactionHashUtil.sha256(combined);
        // Take first 32 characters (256 bits)
        return hash.substring(0, 32);
    }

    /**
     * Create payload map
     */
    public static Map<String, Object> createPayload(
            String senderPhone,
            String recipientPhone,
            String amount,
            String description,
            Map<String, Object> metadata) {

        return Map.of(
                "senderPhone", senderPhone,
                "recipientPhone", recipientPhone,
                "amount", amount,
                "description", description != null ? description : "",
                "metadata", metadata != null ? metadata : Map.of()
        );
    }

    /**
     * Parse payload from JSON string
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse payload", e);
            throw new RuntimeException("Payload parsing failed", e);
        }
    }
}
