package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment request payload for QR code generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recipientId;
    private String recipientPhoneNumber;
    private String recipientName;
    private BigDecimal amount;
    private String note;
    private LocalDateTime timestamp;
    private LocalDateTime expiresAt;
    private String nonce;
    private String paymentRequestId;
    private String signature;

    /**
     * Check if payment request has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Get time remaining in seconds
     */
    public long getTimeRemainingSeconds() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }
}
