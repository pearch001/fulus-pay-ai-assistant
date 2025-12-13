package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Transaction responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private UUID id;
    private UUID userId;
    private TransactionType type;
    private TransactionCategory category;
    private BigDecimal amount;
    private String description;
    private BigDecimal balanceAfter;
    private String reference;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    /**
     * Convert Transaction entity to DTO
     */
    public static TransactionDTO fromEntity(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .balanceAfter(transaction.getBalanceAfter())
                .reference(transaction.getReference())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
