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
 * DTO for offline transaction sync result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    private String userId;
    private boolean success;
    private Integer totalTransactions;
    private Integer successCount;
    private Integer failedCount;
    private Integer conflictCount;

    private String lastSyncedHash;
    private LocalDateTime syncTimestamp;

    @Builder.Default
    private List<SyncedTransaction> syncedTransactions = new ArrayList<>();

    @Builder.Default
    private List<FailedTransaction> failedTransactions = new ArrayList<>();

    @Builder.Default
    private List<ConflictInfo> conflicts = new ArrayList<>();

    private BigDecimal finalBalance;
    private String errorMessage;

    /**
     * Add synced transaction
     */
    public void addSyncedTransaction(SyncedTransaction transaction) {
        this.syncedTransactions.add(transaction);
        this.successCount++;
    }

    /**
     * Add failed transaction
     */
    public void addFailedTransaction(FailedTransaction transaction) {
        this.failedTransactions.add(transaction);
        this.failedCount++;
    }

    /**
     * Add conflict
     */
    public void addConflict(ConflictInfo conflict) {
        this.conflicts.add(conflict);
        this.conflictCount++;
    }

    /**
     * Check if sync was partially successful
     */
    public boolean isPartialSuccess() {
        return successCount > 0 && failedCount > 0;
    }

    /**
     * Check if sync had no issues
     */
    public boolean isCompleteSuccess() {
        return successCount > 0 && failedCount == 0 && conflictCount == 0;
    }

    /**
     * Factory method for complete failure
     */
    public static SyncResult failure(String userId, int totalTransactions, String errorMessage) {
        return SyncResult.builder()
                .userId(userId)
                .success(false)
                .totalTransactions(totalTransactions)
                .successCount(0)
                .failedCount(totalTransactions)
                .conflictCount(0)
                .syncTimestamp(LocalDateTime.now())
                .errorMessage(errorMessage)
                .syncedTransactions(new ArrayList<>())
                .failedTransactions(new ArrayList<>())
                .conflicts(new ArrayList<>())
                .build();
    }

    /**
     * Factory method for successful sync
     */
    public static SyncResult success(String userId, int syncedCount, String lastHash, BigDecimal finalBalance) {
        return SyncResult.builder()
                .userId(userId)
                .success(true)
                .totalTransactions(syncedCount)
                .successCount(syncedCount)
                .failedCount(0)
                .conflictCount(0)
                .lastSyncedHash(lastHash)
                .syncTimestamp(LocalDateTime.now())
                .finalBalance(finalBalance)
                .syncedTransactions(new ArrayList<>())
                .failedTransactions(new ArrayList<>())
                .conflicts(new ArrayList<>())
                .build();
    }

    /**
     * Synced transaction info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncedTransaction {
        private UUID offlineTransactionId;
        private UUID onlineTransactionId;
        private String transactionHash;
        private String senderPhoneNumber;
        private String recipientPhoneNumber;
        private BigDecimal amount;
        private LocalDateTime offlineTimestamp;
        private LocalDateTime syncedTimestamp;
        private String description;
    }

    /**
     * Failed transaction info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedTransaction {
        private UUID offlineTransactionId;
        private String transactionHash;
        private String senderPhoneNumber;
        private String recipientPhoneNumber;
        private BigDecimal amount;
        private String failureReason;
        private String errorDetails;
        private LocalDateTime attemptedAt;
        private Integer transactionIndex;
    }

    /**
     * Conflict information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private UUID conflictId;
        private UUID transactionId;
        private String transactionHash;
        private String conflictType;
        private String description;
        private BigDecimal expectedBalance;
        private BigDecimal actualBalance;
        private LocalDateTime detectedAt;
        private boolean autoResolutionAttempted;
        private String resolutionStatus;
    }
}
