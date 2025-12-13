package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.KeyPairDTO;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.UsedNonce;
import com.fulus.ai.assistant.entity.UserKeyPair;
import com.fulus.ai.assistant.repository.UsedNonceRepository;
import com.fulus.ai.assistant.repository.UserKeyPairRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for cryptographic operations on offline transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoUtilityService {

    private final UsedNonceRepository usedNonceRepository;
    private final UserKeyPairRepository userKeyPairRepository;

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String ECDSA_ALGORITHM = "EC";
    private static final String SIGNATURE_ALGORITHM_RSA = "SHA256withRSA";
    private static final String SIGNATURE_ALGORITHM_ECDSA = "SHA256withECDSA";
    private static final int RSA_KEY_SIZE = 2048; // 2048 bits for RSA
    private static final String ECDSA_CURVE = "secp256r1"; // P-256 curve for ECDSA
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /**
     * 1. Generate SHA-256 transaction hash
     *
     * @param transaction The offline transaction
     * @return Hex-encoded hash string
     */
    public String generateTransactionHash(OfflineTransaction transaction) {
        try {
            log.debug("Generating transaction hash for sender: {}, recipient: {}, amount: {}",
                    transaction.getSenderPhoneNumber(), transaction.getRecipientPhoneNumber(),
                    transaction.getAmount());

            // Concatenate transaction data
            String data = transaction.getSenderPhoneNumber() +
                    transaction.getRecipientPhoneNumber() +
                    transaction.getAmount().toPlainString() +
                    transaction.getTimestamp().format(TIMESTAMP_FORMATTER) +
                    transaction.getNonce() +
                    (transaction.getPreviousHash() != null ? transaction.getPreviousHash() : getGenesisHash());

            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            String hash = bytesToHex(hashBytes);

            log.debug("Transaction hash generated: {}", hash);
            return hash;

        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not available: {}", HASH_ALGORITHM, e);
            throw new RuntimeException("Failed to generate transaction hash: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating transaction hash", e);
            throw new RuntimeException("Failed to generate transaction hash: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Validate transaction signature
     *
     * @param payload   The transaction payload to verify
     * @param signature The signature to validate
     * @param publicKey The public key in Base64 format
     * @return true if signature is valid, false otherwise
     */
    public boolean validateTransactionSignature(String payload, String signature, String publicKey) {
        try {
            log.debug("Validating transaction signature for payload length: {}", payload.length());

            // Decode public key
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);

            // Try RSA first, then ECDSA
            boolean isValid = false;

            try {
                isValid = validateRSASignature(payload, signature, publicKeyBytes);
                log.debug("RSA signature validation: {}", isValid);
            } catch (Exception e) {
                log.debug("RSA validation failed, trying ECDSA: {}", e.getMessage());
                try {
                    isValid = validateECDSASignature(payload, signature, publicKeyBytes);
                    log.debug("ECDSA signature validation: {}", isValid);
                } catch (Exception e2) {
                    log.error("Both RSA and ECDSA signature validation failed", e2);
                    return false;
                }
            }

            if (!isValid) {
                log.warn("Signature validation failed - possible tampering detected");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating transaction signature", e);
            return false;
        }
    }

    /**
     * 3. Generate key pair for user
     *
     * @return KeyPairDTO with public and private keys
     */
    @Transactional
    public KeyPairDTO generateKeyPair() {
        return generateKeyPair(RSA_ALGORITHM);
    }

    /**
     * Generate key pair with specific algorithm
     *
     * @param algorithm RSA or ECDSA
     * @return KeyPairDTO with public and private keys
     */
    @Transactional
    public KeyPairDTO generateKeyPair(String algorithm) {
        try {
            log.info("Generating {} key pair", algorithm);

            KeyPair keyPair;
            int keySize;

            if (RSA_ALGORITHM.equalsIgnoreCase(algorithm)) {
                // Generate RSA key pair
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
                keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
                keyPair = keyGen.generateKeyPair();
                keySize = RSA_KEY_SIZE;

            } else if (ECDSA_ALGORITHM.equalsIgnoreCase(algorithm) || "ECDSA".equalsIgnoreCase(algorithm)) {
                // Generate ECDSA key pair
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ECDSA_ALGORITHM);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec(ECDSA_CURVE);
                keyGen.initialize(ecSpec, new SecureRandom());
                keyPair = keyGen.generateKeyPair();
                keySize = 256; // P-256 curve is 256 bits

            } else {
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }

            // Encode keys to Base64
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            String keyId = UUID.randomUUID().toString();

            KeyPairDTO keyPairDTO = KeyPairDTO.builder()
                    .keyId(keyId)
                    .publicKey(publicKeyBase64)
                    .privateKey(privateKeyBase64)
                    .algorithm(algorithm)
                    .keySize(keySize)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusYears(2)) // 2 years expiry
                    .format("PKCS8/X509")
                    .build();

            log.info("Key pair generated successfully: keyId={}, algorithm={}, keySize={}",
                    keyId, algorithm, keySize);

            return keyPairDTO;

        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available: {}", algorithm, e);
            throw new RuntimeException("Failed to generate key pair: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating key pair", e);
            throw new RuntimeException("Failed to generate key pair: " + e.getMessage(), e);
        }
    }

    /**
     * 4. Sign transaction payload
     *
     * @param payload    The payload to sign
     * @param privateKey The private key in Base64 format
     * @return Base64-encoded signature
     */
    public String signTransaction(String payload, String privateKey) {
        return signTransaction(payload, privateKey, RSA_ALGORITHM);
    }

    /**
     * Sign transaction with specific algorithm
     *
     * @param payload    The payload to sign
     * @param privateKey The private key in Base64 format
     * @param algorithm  RSA or ECDSA
     * @return Base64-encoded signature
     */
    public String signTransaction(String payload, String privateKey, String algorithm) {
        try {
            log.debug("Signing transaction payload with {} algorithm, payload length: {}",
                    algorithm, payload.length());

            // Decode private key
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);

            String signature;

            if (RSA_ALGORITHM.equalsIgnoreCase(algorithm)) {
                signature = signWithRSA(payload, privateKeyBytes);
            } else if (ECDSA_ALGORITHM.equalsIgnoreCase(algorithm) || "ECDSA".equalsIgnoreCase(algorithm)) {
                signature = signWithECDSA(payload, privateKeyBytes);
            } else {
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }

            log.debug("Transaction signed successfully, signature length: {}", signature.length());
            return signature;

        } catch (Exception e) {
            log.error("Error signing transaction", e);
            throw new RuntimeException("Failed to sign transaction: " + e.getMessage(), e);
        }
    }

    /**
     * 5. Validate nonce (prevent replay attacks)
     *
     * @param userId The user ID
     * @param nonce  The nonce to validate
     * @return true if nonce is valid (not used before), false if already used
     */
    @Transactional
    public boolean validateNonce(String userId, String nonce) {
        try {
            log.debug("Validating nonce for user: {}, nonce: {}", userId, nonce);

            UUID userUuid = UUID.fromString(userId);

            // Check if nonce exists and is not expired
            boolean nonceExists = usedNonceRepository.existsByNonceAndNotExpired(nonce, LocalDateTime.now());

            if (nonceExists) {
                log.warn("SECURITY ALERT: Nonce replay attack detected! User: {}, nonce: {}", userId, nonce);
                return false;
            }

            log.debug("Nonce validation passed: {}", nonce);
            return true;

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error validating nonce", e);
            return false;
        }
    }

    /**
     * Mark nonce as used
     *
     * @param userId          The user ID
     * @param nonce           The nonce
     * @param transactionHash The transaction hash using this nonce
     */
    @Transactional
    public void markNonceAsUsed(String userId, String nonce, String transactionHash) {
        try {
            UUID userUuid = UUID.fromString(userId);

            UsedNonce usedNonce = UsedNonce.create(nonce, userUuid, transactionHash);
            usedNonceRepository.save(usedNonce);

            log.debug("Nonce marked as used: {} for transaction: {}", nonce, transactionHash);

        } catch (Exception e) {
            log.error("Error marking nonce as used", e);
            throw new RuntimeException("Failed to mark nonce as used: " + e.getMessage(), e);
        }
    }

    /**
     * Store user's public key
     *
     * @param userId    The user ID
     * @param keyPair   The key pair DTO
     */
    @Transactional
    public void storePublicKey(UUID userId, KeyPairDTO keyPair) {
        try {
            log.info("Storing public key for user: {}, keyId: {}", userId, keyPair.getKeyId());

            // Check if user already has an active key
            userKeyPairRepository.findActiveKeyPairByUserId(userId, LocalDateTime.now())
                    .ifPresent(existingKey -> {
                        log.info("Revoking existing key for user: {}", userId);
                        existingKey.revoke("Replaced with new key");
                        userKeyPairRepository.save(existingKey);
                    });

            // Store new key
            UserKeyPair userKeyPair = UserKeyPair.create(
                    userId,
                    keyPair.getPublicKey(),
                    keyPair.getAlgorithm(),
                    keyPair.getKeySize()
            );
            userKeyPair.setKeyId(keyPair.getKeyId());
            userKeyPair.setExpiresAt(keyPair.getExpiresAt());

            userKeyPairRepository.save(userKeyPair);

            log.info("Public key stored successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error storing public key", e);
            throw new RuntimeException("Failed to store public key: " + e.getMessage(), e);
        }
    }

    /**
     * Get user's public key
     *
     * @param userId The user ID
     * @return Public key in Base64 format, or null if not found
     */
    public String getUserPublicKey(UUID userId) {
        return userKeyPairRepository.findActiveKeyPairByUserId(userId, LocalDateTime.now())
                .map(UserKeyPair::getPublicKey)
                .orElse(null);
    }

    /**
     * Cleanup expired nonces
     * Should be called periodically (e.g., daily cron job)
     */
    @Transactional
    public int cleanupExpiredNonces() {
        log.info("Cleaning up expired nonces");
        int deletedCount = usedNonceRepository.deleteExpiredNonces(LocalDateTime.now());
        log.info("Cleaned up {} expired nonces", deletedCount);
        return deletedCount;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate RSA signature
     */
    private boolean validateRSASignature(String payload, String signature, byte[] publicKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM_RSA);
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return sig.verify(signatureBytes);
    }

    /**
     * Validate ECDSA signature
     */
    private boolean validateECDSASignature(String payload, String signature, byte[] publicKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(ECDSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM_ECDSA);
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return sig.verify(signatureBytes);
    }

    /**
     * Sign with RSA
     */
    private String signWithRSA(String payload, byte[] privateKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM_RSA);
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Sign with ECDSA
     */
    private String signWithECDSA(String payload, byte[] privateKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(ECDSA_ALGORITHM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM_ECDSA);
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Get genesis hash for first transaction
     */
    private String getGenesisHash() {
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }
}
