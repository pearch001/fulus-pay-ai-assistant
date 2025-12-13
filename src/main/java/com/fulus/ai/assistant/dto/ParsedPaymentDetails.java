package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for parsed payment details from QR code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedPaymentDetails {

    private boolean valid;
    private String paymentRequestId;
    private String recipientId;
    private String recipientPhoneNumber;
    private String recipientName;
    private BigDecimal amount;
    private String note;
    private LocalDateTime timestamp;
    private LocalDateTime expiresAt;
    private Long timeRemainingSeconds;
    private boolean expired;
    private boolean signatureValid;
    private String errorMessage;

    /**
     * Factory method for invalid QR code
     */
    public static ParsedPaymentDetails invalid(String errorMessage) {
        return ParsedPaymentDetails.builder()
                .valid(false)
                .signatureValid(false)
                .expired(true)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Factory method for valid payment details
     */
    public static ParsedPaymentDetails valid(PaymentRequestPayload payload) {
        return ParsedPaymentDetails.builder()
                .valid(true)
                .paymentRequestId(payload.getPaymentRequestId())
                .recipientId(payload.getRecipientId())
                .recipientPhoneNumber(payload.getRecipientPhoneNumber())
                .recipientName(payload.getRecipientName())
                .amount(payload.getAmount())
                .note(payload.getNote())
                .timestamp(payload.getTimestamp())
                .expiresAt(payload.getExpiresAt())
                .timeRemainingSeconds(payload.getTimeRemainingSeconds())
                .expired(payload.isExpired())
                .signatureValid(true)
                .build();
    }
}
