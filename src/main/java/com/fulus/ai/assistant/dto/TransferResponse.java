package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private boolean success;
    private String message;
    private UUID transactionId;
    private String reference;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String recipientName;
    private String recipientAccount;
    private String transferType; // INTERNAL or INTER_BANK
    private LocalDateTime timestamp;

    public static TransferResponse success(UUID transactionId, String reference, BigDecimal amount,
                                          BigDecimal balanceAfter, String recipientName,
                                          String recipientAccount, String transferType) {
        return TransferResponse.builder()
                .success(true)
                .message("Transfer successful")
                .transactionId(transactionId)
                .reference(reference)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .recipientName(recipientName)
                .recipientAccount(recipientAccount)
                .transferType(transferType)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static TransferResponse failure(String message) {
        return TransferResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
