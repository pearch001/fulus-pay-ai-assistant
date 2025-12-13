package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.enums.TransactionStatus;
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
public class TransactionResult {

    private boolean success;
    private String message;
    private UUID transactionId;
    private String reference;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private TransactionStatus status;
    private LocalDateTime timestamp;

    // Convenience method to create success result from transaction
    public static TransactionResult success(Transaction transaction, BigDecimal newBalance, String message) {
        return TransactionResult.builder()
                .success(true)
                .message(message)
                .transactionId(transaction.getId())
                .reference(transaction.getReference())
                .amount(transaction.getAmount())
                .newBalance(newBalance)
                .status(transaction.getStatus())
                .timestamp(transaction.getCreatedAt())
                .build();
    }

    // Convenience method to create failure result
    public static TransactionResult failure(String message) {
        return TransactionResult.builder()
                .success(false)
                .message(message)
                .status(TransactionStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Get the transaction entity (for backward compatibility)
    public Transaction getTransaction() {
        if (this.transactionId == null) {
            return null;
        }

        Transaction tx = new Transaction();
        tx.setId(this.transactionId);
        tx.setReference(this.reference);
        tx.setAmount(this.amount);
        tx.setStatus(this.status);
        tx.setCreatedAt(this.timestamp);
        return tx;
    }
}
