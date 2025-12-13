package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for offline transaction data in API requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineTransactionDTO {

    @NotBlank(message = "Sender phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String senderPhoneNumber;

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String recipientPhoneNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Transaction hash is required")
    private String transactionHash;

    @NotBlank(message = "Previous hash is required")
    private String previousHash;

    @NotBlank(message = "Signature is required")
    private String signature;

    @NotBlank(message = "Nonce is required")
    private String nonce;

    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    private String payload;

    private BigDecimal senderOfflineBalance;

    private String description;
}
