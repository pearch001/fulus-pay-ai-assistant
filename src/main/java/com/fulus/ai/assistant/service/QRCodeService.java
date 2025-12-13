package com.fulus.ai.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.util.NonceGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating and parsing QR codes for offline payments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final UserRepository userRepository;
    private final CryptoUtilityService cryptoUtilityService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Redis key prefix for payment requests
    private static final String PAYMENT_REQUEST_KEY_PREFIX = "payment:request:";

    // QR Code configuration
    private static final int DEFAULT_QR_SIZE = 300;
    private static final int MAX_QR_SIZE = 1000;
    private static final int MIN_QR_SIZE = 150;
    private static final int QR_EXPIRY_MINUTES = 5;

    /**
     * 1. Generate payment QR code
     *
     * @param userId User ID of the payment recipient
     * @param amount Payment amount
     * @param note   Optional payment note
     * @return QRCodeResponse with Base64 encoded QR image
     */
    public QRCodeResponse generatePaymentQRCode(String userId, double amount, String note) {
        return generatePaymentQRCode(userId, amount, note, DEFAULT_QR_SIZE);
    }

    /**
     * Generate payment QR code with custom size
     *
     * @param userId  User ID of the payment recipient
     * @param amount  Payment amount
     * @param note    Optional payment note
     * @param qrSize  QR code size in pixels
     * @return QRCodeResponse with Base64 encoded QR image
     */
    public QRCodeResponse generatePaymentQRCode(String userId, double amount, String note, int qrSize) {
        try {
            log.info("Generating payment QR code for user: {}, amount: {}", userId, amount);

            // Validate QR size
            qrSize = validateQRSize(qrSize);

            // Get user details
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Get user's public key (for verification later)
            String publicKey = cryptoUtilityService.getUserPublicKey(userUuid);
            if (publicKey == null) {
                throw new IllegalStateException("User does not have a registered key pair. Please register keys first.");
            }

            // Create payment request payload
            String paymentRequestId = UUID.randomUUID().toString();
            String nonce = NonceGenerator.generateUUIDNonce();
            LocalDateTime timestamp = LocalDateTime.now();
            LocalDateTime expiresAt = timestamp.plusMinutes(QR_EXPIRY_MINUTES);

            PaymentRequestPayload payload = PaymentRequestPayload.builder()
                    .recipientId(userId)
                    .recipientPhoneNumber(user.getPhoneNumber())
                    .recipientName(user.getFullName())
                    .amount(BigDecimal.valueOf(amount))
                    .note(note)
                    .timestamp(timestamp)
                    .expiresAt(expiresAt)
                    .nonce(nonce)
                    .paymentRequestId(paymentRequestId)
                    .build();

            // Note: In a real implementation, the user's device would sign this with their private key
            // For now, we'll create an unsigned payload or sign with a server key for demo purposes
            // In production, this QR should be generated on the mobile device with the user's private key

            // Sign the payload (simulated - in production this would be done on mobile device)
            // For demo purposes, we'll just include the payload without signature verification requirement
            payload.setSignature("UNSIGNED"); // Mobile app will need to sign when making payment

            // Store in Redis with expiry (TTL based on QR_EXPIRY_MINUTES)
            String redisKey = PAYMENT_REQUEST_KEY_PREFIX + paymentRequestId;
            redisTemplate.opsForValue().set(redisKey, payload, QR_EXPIRY_MINUTES, TimeUnit.MINUTES);
            log.debug("Payment request stored in Redis with key: {} (TTL: {} minutes)", redisKey, QR_EXPIRY_MINUTES);

            // Serialize full payload to JSON (with placeholder signature)
            String qrData = objectMapper.writeValueAsString(payload);

            // Generate QR code image
            String qrCodeImage = generateQRCodeImage(qrData, qrSize);

            QRCodeResponse response = QRCodeResponse.success(
                    qrCodeImage,
                    paymentRequestId,
                    qrData,
                    expiresAt,
                    qrSize
            );

            log.info("QR code generated successfully: paymentRequestId={}, size={}x{}",
                    paymentRequestId, qrSize, qrSize);

            return response;

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Error serializing payment payload", e);
            throw new RuntimeException("Failed to generate QR code: JSON serialization error", e);
        } catch (Exception e) {
            log.error("Error generating payment QR code", e);
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Parse and validate payment QR code
     *
     * @param qrData QR code data (JSON string)
     * @return ParsedPaymentDetails with validation results
     */
    public ParsedPaymentDetails parsePaymentQRCode(String qrData) {
        try {
            log.debug("Parsing payment QR code, data length: {}", qrData.length());

            // Deserialize JSON payload
            PaymentRequestPayload payload = objectMapper.readValue(qrData, PaymentRequestPayload.class);

            log.debug("QR code parsed: paymentRequestId={}, amount={}, recipient={}",
                    payload.getPaymentRequestId(), payload.getAmount(), payload.getRecipientPhoneNumber());

            // Validate expiry
            if (payload.isExpired()) {
                log.warn("Payment request expired: {}", payload.getPaymentRequestId());
                return ParsedPaymentDetails.invalid("Payment request has expired");
            }

            // Check if payment request exists in Redis
            String redisKey = PAYMENT_REQUEST_KEY_PREFIX + payload.getPaymentRequestId();
            PaymentRequestPayload cachedPayload = (PaymentRequestPayload) redisTemplate.opsForValue().get(redisKey);
            if (cachedPayload == null) {
                log.warn("Payment request not found in Redis or expired: {}", payload.getPaymentRequestId());
                return ParsedPaymentDetails.invalid("Payment request not found or has expired");
            }

            // Verify signature (if present)
            boolean signatureValid = true;
            if (payload.getSignature() != null && !"UNSIGNED".equals(payload.getSignature())) {
                // Get recipient's public key
                String publicKey = cryptoUtilityService.getUserPublicKey(UUID.fromString(payload.getRecipientId()));
                if (publicKey == null) {
                    log.warn("Public key not found for recipient: {}", payload.getRecipientId());
                    return ParsedPaymentDetails.invalid("Recipient public key not found");
                }

                // Verify signature
                // Create payload string without signature for verification
                PaymentRequestPayload payloadForVerification = PaymentRequestPayload.builder()
                        .recipientId(payload.getRecipientId())
                        .recipientPhoneNumber(payload.getRecipientPhoneNumber())
                        .recipientName(payload.getRecipientName())
                        .amount(payload.getAmount())
                        .note(payload.getNote())
                        .timestamp(payload.getTimestamp())
                        .expiresAt(payload.getExpiresAt())
                        .nonce(payload.getNonce())
                        .paymentRequestId(payload.getPaymentRequestId())
                        .build();

                String payloadString = objectMapper.writeValueAsString(payloadForVerification);
                signatureValid = cryptoUtilityService.validateTransactionSignature(
                        payloadString, payload.getSignature(), publicKey);

                if (!signatureValid) {
                    log.warn("SECURITY ALERT: Invalid signature for payment request: {}",
                            payload.getPaymentRequestId());
                    return ParsedPaymentDetails.invalid("Invalid signature - QR code may be tampered");
                }
            }

            // Validate nonce hasn't been used
            if (!cryptoUtilityService.validateNonce(payload.getRecipientId(), payload.getNonce())) {
                log.warn("SECURITY ALERT: Nonce already used for payment request: {}",
                        payload.getPaymentRequestId());
                return ParsedPaymentDetails.invalid("Invalid nonce - possible replay attack");
            }

            // Validate amount
            if (payload.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid amount in payment request: {}", payload.getAmount());
                return ParsedPaymentDetails.invalid("Invalid payment amount");
            }

            log.info("Payment QR code validated successfully: {}", payload.getPaymentRequestId());

            return ParsedPaymentDetails.valid(payload);

        } catch (JsonProcessingException e) {
            log.error("Error parsing QR code JSON", e);
            return ParsedPaymentDetails.invalid("Invalid QR code format");
        } catch (Exception e) {
            log.error("Error parsing payment QR code", e);
            return ParsedPaymentDetails.invalid("Failed to parse QR code: " + e.getMessage());
        }
    }

    /**
     * Mark payment request as used (after successful payment)
     *
     * @param paymentRequestId The payment request ID
     */
    public void markPaymentRequestUsed(String paymentRequestId) {
        String redisKey = PAYMENT_REQUEST_KEY_PREFIX + paymentRequestId;
        PaymentRequestPayload payload = (PaymentRequestPayload) redisTemplate.opsForValue().get(redisKey);

        if (payload != null) {
            // Delete from Redis
            redisTemplate.delete(redisKey);

            // Mark nonce as used
            cryptoUtilityService.markNonceAsUsed(
                    payload.getRecipientId(),
                    payload.getNonce(),
                    paymentRequestId
            );
            log.info("Payment request marked as used and removed from Redis: {}", paymentRequestId);
        } else {
            log.warn("Payment request not found in Redis when marking as used: {}", paymentRequestId);
        }
    }

    /**
     * Get pending payment request
     *
     * @param paymentRequestId The payment request ID
     * @return PaymentRequestPayload or null if not found
     */
    public PaymentRequestPayload getPendingPaymentRequest(String paymentRequestId) {
        String redisKey = PAYMENT_REQUEST_KEY_PREFIX + paymentRequestId;
        return (PaymentRequestPayload) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Cleanup expired payment requests
     * Note: Redis handles automatic expiry via TTL, so this method is mainly for manual cleanup
     * Should be called periodically if needed
     */
    public int cleanupExpiredRequests() {
        log.debug("Cleaning up expired payment requests from Redis");
        int removedCount = 0;

        try {
            // Get all payment request keys
            Set<String> keys = redisTemplate.keys(PAYMENT_REQUEST_KEY_PREFIX + "*");

            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    PaymentRequestPayload payload = (PaymentRequestPayload) redisTemplate.opsForValue().get(key);

                    if (payload != null && payload.isExpired()) {
                        redisTemplate.delete(key);
                        removedCount++;
                        log.debug("Removed expired payment request: {}", key);
                    }
                }
            }

            log.info("Cleaned up {} expired payment requests from Redis", removedCount);
        } catch (Exception e) {
            log.error("Error cleaning up expired payment requests from Redis", e);
        }

        return removedCount;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Generate QR code image from data
     *
     * @param data   QR code data
     * @param size   Image size in pixels
     * @return Base64 encoded PNG image
     */
    private String generateQRCodeImage(String data, int size) {
        try {
            // Configure QR code parameters
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // Medium error correction
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Smaller margin for mobile scanning

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to buffered image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64 PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            log.debug("QR code image generated: size={}x{}, data length={}, image size={}KB",
                    size, size, data.length(), imageBytes.length / 1024);

            return base64Image;

        } catch (WriterException e) {
            log.error("Error encoding QR code", e);
            throw new RuntimeException("Failed to encode QR code: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error writing QR code image", e);
            throw new RuntimeException("Failed to generate QR code image: " + e.getMessage(), e);
        }
    }

    /**
     * Validate and normalize QR code size
     *
     * @param size Requested size
     * @return Validated size
     */
    private int validateQRSize(int size) {
        if (size < MIN_QR_SIZE) {
            log.debug("QR size {} too small, using minimum: {}", size, MIN_QR_SIZE);
            return MIN_QR_SIZE;
        }
        if (size > MAX_QR_SIZE) {
            log.debug("QR size {} too large, using maximum: {}", size, MAX_QR_SIZE);
            return MAX_QR_SIZE;
        }
        return size;
    }

    /**
     * Get statistics about pending requests
     */
    public Map<String, Object> getStatistics() {
        int totalPending = 0;
        int expired = 0;

        try {
            // Get all payment request keys from Redis
            Set<String> keys = redisTemplate.keys(PAYMENT_REQUEST_KEY_PREFIX + "*");

            if (keys != null) {
                totalPending = keys.size();

                for (String key : keys) {
                    PaymentRequestPayload payload = (PaymentRequestPayload) redisTemplate.opsForValue().get(key);
                    if (payload != null && payload.isExpired()) {
                        expired++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting statistics from Redis", e);
        }

        return Map.of(
                "totalPending", totalPending,
                "expired", expired,
                "active", totalPending - expired
        );
    }
}
