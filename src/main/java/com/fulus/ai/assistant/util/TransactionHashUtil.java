package com.fulus.ai.assistant.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating SHA-256 hashes for offline transactions
 */
@Slf4j
public class TransactionHashUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Generate SHA-256 hash for a transaction
     *
     * @param senderPhone Sender's phone number
     * @param recipientPhone Recipient's phone number
     * @param amount Transaction amount
     * @param timestamp Transaction timestamp
     * @param nonce Unique nonce
     * @param previousHash Previous transaction hash in chain
     * @return SHA-256 hash as hex string
     */
    public static String generateTransactionHash(
            String senderPhone,
            String recipientPhone,
            String amount,
            LocalDateTime timestamp,
            String nonce,
            String previousHash) {

        // Build the data string
        String data = senderPhone +
                      recipientPhone +
                      amount +
                      timestamp.format(TIMESTAMP_FORMATTER) +
                      nonce +
                      (previousHash != null ? previousHash : getGenesisHash());

        return sha256(data);
    }

    /**
     * Generate SHA-256 hash from string
     *
     * @param data Input data
     * @return SHA-256 hash as hex string
     */
    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    /**
     * Verify transaction hash
     *
     * @param transaction Transaction data
     * @param expectedHash Expected hash
     * @return true if hash matches
     */
    public static boolean verifyTransactionHash(
            String senderPhone,
            String recipientPhone,
            String amount,
            LocalDateTime timestamp,
            String nonce,
            String previousHash,
            String expectedHash) {

        String calculatedHash = generateTransactionHash(
                senderPhone, recipientPhone, amount, timestamp, nonce, previousHash);

        return calculatedHash.equals(expectedHash);
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Get genesis hash (for first transaction in chain)
     */
    public static String getGenesisHash() {
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }

    /**
     * Validate hash format (64 hex characters)
     */
    public static boolean isValidHashFormat(String hash) {
        if (hash == null || hash.length() != 64) {
            return false;
        }
        return hash.matches("^[a-f0-9]{64}$");
    }

    /**
     * Generate hash for payload (for encrypted storage)
     */
    public static String hashPayload(String payload) {
        return sha256(payload);
    }
}
