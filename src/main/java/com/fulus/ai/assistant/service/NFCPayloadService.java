package com.fulus.ai.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.SyncStatus;
import com.fulus.ai.assistant.repository.OfflineTransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.util.NonceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Service for creating and validating NFC payment payloads
 * Defines structured format for offline NFC transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NFCPayloadService {

    private final UserRepository userRepository;
    private final OfflineTransactionRepository offlineTransactionRepository;
    private final CryptoUtilityService cryptoUtilityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // NFC Configuration
    private static final String PAYLOAD_VERSION = "1.0";
    private static final String PAYLOAD_TYPE = "OFFLINE_PAYMENT";
    private static final String DEFAULT_CURRENCY = "NGN";
    private static final int MAX_PAYLOAD_SIZE = 4096; // 4KB for NFC compatibility
    private static final int TIMESTAMP_VALIDITY_MINUTES = 5; // Timestamp freshness window

    /**
     * 1. Create NFC payload for offline payment
     *
     * @param senderId    Sender user ID
     * @param recipientId Recipient user ID
     * @param amount      Payment amount
     * @param note        Optional payment note
     * @return JSON string payload (max 4KB for NFC)
     */
    public String createNFCPayload(String senderId, String recipientId, double amount, String note) {
        try {
            log.info("Creating NFC payload: sender={}, recipient={}, amount={}",
                    senderId, recipientId, amount);

            // Get sender and recipient details
            UUID senderUuid = UUID.fromString(senderId);
            UUID recipientUuid = UUID.fromString(recipientId);

            User sender = userRepository.findById(senderUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderId));

            User recipient = userRepository.findById(recipientUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + recipientId));

            // Get sender's public key
            String senderPublicKey = cryptoUtilityService.getUserPublicKey(senderUuid);
            if (senderPublicKey == null) {
                throw new IllegalStateException("Sender does not have a registered key pair");
            }

            // Get recipient's public key
            String recipientPublicKey = cryptoUtilityService.getUserPublicKey(recipientUuid);
            if (recipientPublicKey == null) {
                throw new IllegalStateException("Recipient does not have a registered key pair");
            }

            // Get sender's device ID (simulated - in production from mobile app)
            String deviceId = "DEVICE-" + senderUuid.toString().substring(0, 8);

            // Generate nonce
            String nonce = NonceGenerator.generateUUIDNonce();

            // Get current timestamp
            Long timestamp = Instant.now().toEpochMilli();

            // Get previous transaction hash for chain
            String previousHash = getPreviousTransactionHash(sender.getPhoneNumber());

            // Build payload components
            NFCSenderDTO senderDTO = NFCSenderDTO.builder()
                    .phoneNumber(sender.getPhoneNumber())
                    .publicKey(senderPublicKey)
                    .deviceId(deviceId)
                    .build();

            NFCRecipientDTO recipientDTO = NFCRecipientDTO.builder()
                    .phoneNumber(recipient.getPhoneNumber())
                    .publicKey(recipientPublicKey)
                    .build();

            NFCTransactionDTO transactionDTO = NFCTransactionDTO.builder()
                    .amount(BigDecimal.valueOf(amount))
                    .currency(DEFAULT_CURRENCY)
                    .timestamp(timestamp)
                    .nonce(nonce)
                    .note(note)
                    .build();

            // Generate transaction hash
            String transactionHash = generateTransactionHash(
                    sender.getPhoneNumber(),
                    recipient.getPhoneNumber(),
                    BigDecimal.valueOf(amount),
                    timestamp,
                    nonce,
                    previousHash
            );

            // Sign the transaction hash
            // Note: In production, this should be done on the mobile device with user's private key
            // For now, we'll create a placeholder signature
            String signature = "SIGNATURE_TO_BE_GENERATED_ON_MOBILE";

            NFCSecurityDTO securityDTO = NFCSecurityDTO.builder()
                    .hash(transactionHash)
                    .previousHash(previousHash)
                    .signature(signature)
                    .build();

            // Build complete payload
            NFCPayloadDTO payload = NFCPayloadDTO.builder()
                    .version(PAYLOAD_VERSION)
                    .type(PAYLOAD_TYPE)
                    .sender(senderDTO)
                    .recipient(recipientDTO)
                    .transaction(transactionDTO)
                    .security(securityDTO)
                    .build();

            // Serialize to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Validate size for NFC compatibility
            int payloadSize = payloadJson.getBytes(StandardCharsets.UTF_8).length;
            if (payloadSize > MAX_PAYLOAD_SIZE) {
                log.warn("NFC payload exceeds size limit: {}KB > 4KB", payloadSize / 1024.0);
                throw new IllegalStateException(
                        String.format("Payload too large for NFC: %d bytes (max %d bytes)",
                                payloadSize, MAX_PAYLOAD_SIZE));
            }

            log.info("NFC payload created successfully: size={}bytes, hash={}",
                    payloadSize, transactionHash);

            return payloadJson;

        } catch (JsonProcessingException e) {
            log.error("Error serializing NFC payload to JSON", e);
            throw new RuntimeException("Failed to create NFC payload: JSON error", e);
        } catch (Exception e) {
            log.error("Error creating NFC payload", e);
            throw new RuntimeException("Failed to create NFC payload: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Validate NFC payload
     *
     * @param payloadJson NFC payload JSON string
     * @return Validation result with details
     */
    public NFCValidationResult validateNFCPayload(String payloadJson) {
        try {
            log.debug("Validating NFC payload, size: {} bytes", payloadJson.getBytes().length);

            NFCValidationResult result = NFCValidationResult.builder()
                    .valid(true)
                    .signatureValid(true)
                    .hashValid(true)
                    .timestampValid(true)
                    .nonceValid(true)
                    .sizeCompatible(true)
                    .versionSupported(true)
                    .errors(new java.util.ArrayList<>())
                    .warnings(new java.util.ArrayList<>())
                    .build();

            // 1. Validate size
            int payloadSize = payloadJson.getBytes(StandardCharsets.UTF_8).length;
            if (payloadSize > MAX_PAYLOAD_SIZE) {
                result.addError(String.format("Payload too large: %d bytes (max %d bytes)",
                        payloadSize, MAX_PAYLOAD_SIZE));
                result.setSizeCompatible(false);
            }

            // 2. Parse JSON
            NFCPayloadDTO payload;
            try {
                payload = objectMapper.readValue(payloadJson, NFCPayloadDTO.class);
                result.setPayload(payload);
            } catch (JsonProcessingException e) {
                log.error("Invalid JSON format in NFC payload", e);
                result.addError("Invalid JSON format");
                return result;
            }

            // 3. Validate version
            if (!PAYLOAD_VERSION.equals(payload.getVersion())) {
                result.addError("Unsupported version: " + payload.getVersion());
                result.setVersionSupported(false);
            }

            // 4. Validate type
            if (!PAYLOAD_TYPE.equals(payload.getType())) {
                result.addError("Invalid payload type: " + payload.getType());
            }

            // 5. Validate required fields
            if (payload.getSender() == null || payload.getRecipient() == null ||
                    payload.getTransaction() == null || payload.getSecurity() == null) {
                result.addError("Missing required payload fields");
                return result;
            }

            // 6. Validate timestamp freshness (within 5 minutes)
            Long timestamp = payload.getTransaction().getTimestamp();
            long now = Instant.now().toEpochMilli();
            long ageMinutes = (now - timestamp) / (1000 * 60);

            if (Math.abs(ageMinutes) > TIMESTAMP_VALIDITY_MINUTES) {
                result.addError(String.format("Timestamp not fresh: %d minutes old (max %d minutes)",
                        Math.abs(ageMinutes), TIMESTAMP_VALIDITY_MINUTES));
                result.setTimestampValid(false);
            }

            // 7. Validate nonce uniqueness
            String nonce = payload.getTransaction().getNonce();
            if (!cryptoUtilityService.validateNonce(
                    getSenderUserId(payload.getSender().getPhoneNumber()), nonce)) {
                result.addError("Nonce already used - replay attack detected");
                result.setNonceValid(false);
            }

            // 8. Validate transaction hash
            String calculatedHash = generateTransactionHash(
                    payload.getSender().getPhoneNumber(),
                    payload.getRecipient().getPhoneNumber(),
                    payload.getTransaction().getAmount(),
                    payload.getTransaction().getTimestamp(),
                    payload.getTransaction().getNonce(),
                    payload.getSecurity().getPreviousHash()
            );

            if (!calculatedHash.equals(payload.getSecurity().getHash())) {
                log.warn("SECURITY ALERT: Transaction hash mismatch - possible tampering");
                result.addError("Hash verification failed - payload may be tampered");
                result.setHashValid(false);
            }

            // 9. Validate signature
            if (!"SIGNATURE_TO_BE_GENERATED_ON_MOBILE".equals(payload.getSecurity().getSignature())) {
                // Verify signature with sender's public key
                boolean signatureValid = cryptoUtilityService.validateTransactionSignature(
                        payload.getSecurity().getHash(),
                        payload.getSecurity().getSignature(),
                        payload.getSender().getPublicKey()
                );

                if (!signatureValid) {
                    log.warn("SECURITY ALERT: Invalid signature - possible forgery");
                    result.addError("Signature verification failed");
                    result.setSignatureValid(false);
                }
            } else {
                result.addWarning("Signature not verified (placeholder signature)");
            }

            // 10. Validate amount
            if (payload.getTransaction().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                result.addError("Invalid amount: must be greater than zero");
            }

            // 11. Validate currency
            if (!DEFAULT_CURRENCY.equals(payload.getTransaction().getCurrency())) {
                result.addWarning("Unsupported currency: " + payload.getTransaction().getCurrency());
            }

            // Final validation status
            result.setValid(result.getErrors().isEmpty());

            if (result.isValid()) {
                log.info("NFC payload validation passed: hash={}", payload.getSecurity().getHash());
            } else {
                log.warn("NFC payload validation failed: {} errors", result.getErrors().size());
            }

            return result;

        } catch (Exception e) {
            log.error("Error validating NFC payload", e);
            return NFCValidationResult.failure("Validation error: " + e.getMessage());
        }
    }

    /**
     * 3. Extract OfflineTransaction from NFC payload
     *
     * @param payloadJson NFC payload JSON string
     * @return OfflineTransaction entity ready for storage
     */
    public OfflineTransaction extractTransactionFromPayload(String payloadJson) {
        try {
            log.debug("Extracting transaction from NFC payload");

            // First validate the payload
            NFCValidationResult validation = validateNFCPayload(payloadJson);
            if (!validation.isValid()) {
                throw new IllegalArgumentException(
                        "Cannot extract transaction from invalid payload: " +
                                String.join(", ", validation.getErrors()));
            }

            NFCPayloadDTO payload = validation.getPayload();

            // Convert timestamp to LocalDateTime
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(payload.getTransaction().getTimestamp()),
                    ZoneOffset.UTC
            );

            // Create OfflineTransaction entity
            OfflineTransaction transaction = new OfflineTransaction();
            transaction.setSenderPhoneNumber(payload.getSender().getPhoneNumber());
            transaction.setRecipientPhoneNumber(payload.getRecipient().getPhoneNumber());
            transaction.setAmount(payload.getTransaction().getAmount());
            transaction.setTransactionHash(payload.getSecurity().getHash());
            transaction.setPreviousHash(payload.getSecurity().getPreviousHash());
            transaction.setSignatureKey(payload.getSecurity().getSignature());
            transaction.setNonce(payload.getTransaction().getNonce());
            transaction.setTimestamp(timestamp);
            transaction.setPayload(payloadJson); // Store raw payload
            transaction.setDescription(payload.getTransaction().getNote());
            transaction.setSyncStatus(SyncStatus.PENDING);
            transaction.setSyncAttempts(0);
            transaction.setCreatedAt(LocalDateTime.now());

            log.info("Transaction extracted from NFC payload: hash={}, amount={}",
                    transaction.getTransactionHash(), transaction.getAmount());

            return transaction;

        } catch (Exception e) {
            log.error("Error extracting transaction from NFC payload", e);
            throw new RuntimeException("Failed to extract transaction: " + e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Generate transaction hash
     */
    private String generateTransactionHash(String senderPhone, String recipientPhone,
                                          BigDecimal amount, Long timestamp, String nonce,
                                          String previousHash) {
        try {
            String data = senderPhone + recipientPhone +
                    amount.toPlainString() + timestamp + nonce +
                    (previousHash != null ? previousHash : getGenesisHash());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hashBytes);

        } catch (Exception e) {
            log.error("Error generating transaction hash", e);
            throw new RuntimeException("Failed to generate hash: " + e.getMessage(), e);
        }
    }

    /**
     * Get previous transaction hash for chain
     */
    private String getPreviousTransactionHash(String senderPhone) {
        return offlineTransactionRepository.findLatestSyncedTransaction(senderPhone)
                .map(OfflineTransaction::getTransactionHash)
                .orElse(getGenesisHash());
    }

    /**
     * Get genesis hash for first transaction
     */
    private String getGenesisHash() {
        return "0000000000000000000000000000000000000000000000000000000000000000";
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
     * Get user ID from phone number
     */
    private String getSenderUserId(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> user.getId().toString())
                .orElse(null);
    }

    /**
     * Validate payload size
     */
    public boolean isPayloadSizeCompatible(String payloadJson) {
        int size = payloadJson.getBytes(StandardCharsets.UTF_8).length;
        return size <= MAX_PAYLOAD_SIZE;
    }

    /**
     * Get maximum payload size
     */
    public int getMaxPayloadSize() {
        return MAX_PAYLOAD_SIZE;
    }

    /**
     * Get payload version
     */
    public String getPayloadVersion() {
        return PAYLOAD_VERSION;
    }
}
