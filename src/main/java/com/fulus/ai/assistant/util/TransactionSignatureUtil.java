package com.fulus.ai.assistant.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * Utility class for generating and verifying transaction signatures
 *
 * Uses HMAC-SHA256 for mock implementation (PoC)
 * In production, use RSA or ECDSA with public/private key pairs
 */
@Slf4j
public class TransactionSignatureUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Generate signature for transaction (mock implementation using HMAC)
     *
     * @param transactionHash Transaction hash to sign
     * @param secretKey User's secret key (in production: use private key)
     * @return Base64 encoded signature
     */
    public static String generateSignature(String transactionHash, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] signatureBytes = mac.doFinal(transactionHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (Exception e) {
            log.error("Failed to generate signature", e);
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    /**
     * Verify transaction signature (mock implementation)
     *
     * @param transactionHash Transaction hash
     * @param signature Signature to verify
     * @param secretKey User's secret key (in production: use public key)
     * @return true if signature is valid
     */
    public static boolean verifySignature(String transactionHash, String signature, String secretKey) {
        try {
            String expectedSignature = generateSignature(transactionHash, secretKey);
            return MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }

    /**
     * Generate user secret key (mock - for development/testing)
     * In production, use proper key generation and secure storage
     *
     * @param phoneNumber User's phone number
     * @param pin User's PIN (hashed)
     * @return Secret key
     */
    public static String generateUserSecretKey(String phoneNumber, String pin) {
        // Mock implementation: combine phone + pin and hash
        String combined = phoneNumber + ":" + pin;
        return TransactionHashUtil.sha256(combined);
    }

    /**
     * Generate key pair for user (for production use)
     * This is a placeholder for RSA/ECDSA key generation
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate key pair", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Sign with RSA private key (for production)
     */
    public static String signWithRSA(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to sign with RSA", e);
            throw new RuntimeException("RSA signing failed", e);
        }
    }

    /**
     * Verify RSA signature (for production)
     */
    public static boolean verifyRSASignature(String data, String signatureBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to verify RSA signature", e);
            return false;
        }
    }

    /**
     * Validate signature format
     */
    public static boolean isValidSignatureFormat(String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(signature);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
