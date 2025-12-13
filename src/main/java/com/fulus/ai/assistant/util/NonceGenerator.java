package com.fulus.ai.assistant.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility class for generating unique nonces (number used once)
 * Prevents replay attacks in offline transactions
 */
public class NonceGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HEX_CHARS = "0123456789abcdef";

    /**
     * Generate UUID-based nonce
     * Most secure and guaranteed unique
     */
    public static String generateUUIDNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate random hex nonce (32 characters)
     */
    public static String generateHexNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * Generate timestamp-based nonce with random suffix
     * Format: {timestamp}-{random}
     */
    public static String generateTimestampNonce() {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomString(16);
        return timestamp + "-" + randomPart;
    }

    /**
     * Generate nonce with user context
     * Format: {phoneNumber}-{timestamp}-{random}
     */
    public static String generateContextualNonce(String phoneNumber) {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomString(8);
        return TransactionHashUtil.sha256(phoneNumber + "-" + timestamp + "-" + randomPart);
    }

    /**
     * Generate random hex string
     */
    private static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(HEX_CHARS.charAt(SECURE_RANDOM.nextInt(HEX_CHARS.length())));
        }
        return result.toString();
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Validate nonce format
     */
    public static boolean isValidNonce(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        // UUID format (32 hex chars) or timestamped format
        return nonce.matches("^[a-f0-9]{32}$") ||
               nonce.matches("^\\d{13}-[a-f0-9]{16}$") ||
               nonce.matches("^[a-f0-9]{64}$");
    }
}
