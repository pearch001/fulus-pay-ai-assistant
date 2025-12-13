package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for pending transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTransactionsResponse {

    private String userId;
    private Integer totalPending;
    private boolean chainValid;
    private String lastSyncedHash;

    @Builder.Default
    private List<PendingTransactionInfo> transactions = new ArrayList<>();

    /**
     * Pending transaction info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingTransactionInfo {
        private UUID id;
        private String transactionHash;
        private String previousHash;
        private String senderPhoneNumber;
        private String recipientPhoneNumber;
        private BigDecimal amount;
        private LocalDateTime timestamp;
        private String syncStatus;
        private Integer syncAttempts;
        private LocalDateTime lastSyncAttempt;
        private String syncError;
        private BigDecimal senderOfflineBalance;
    }
}
