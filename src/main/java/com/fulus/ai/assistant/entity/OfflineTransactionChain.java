package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking offline transaction chain state per user
 *
 * Maintains blockchain-like chain integrity for each user's offline transactions
 */
@Entity
@Table(name = "offline_transaction_chains", indexes = {
    @Index(name = "idx_user_id_chain", columnList = "userId", unique = true),
    @Index(name = "idx_chain_valid", columnList = "chainValid")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineTransactionChain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User ID (foreign key to users table)
     */
    @Column(nullable = false, unique = true)
    private UUID userId;

    /**
     * User phone number (for quick lookups)
     */
    @Column(nullable = false, length = 20)
    private String userPhoneNumber;

    /**
     * Hash of the last successfully synced transaction
     * Used as the previous hash for the next offline transaction
     */
    @Column(length = 64)
    private String lastSyncedHash;

    /**
     * Timestamp of last successful sync
     */
    @Column
    private LocalDateTime lastSyncedAt;

    /**
     * Number of pending offline transactions
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer pendingCount = 0;

    /**
     * Chain validity flag
     * False if hash chain integrity is compromised
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean chainValid = true;

    /**
     * Total number of offline transactions in the chain
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalTransactions = 0;

    /**
     * Number of successfully synced transactions
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer syncedCount = 0;

    /**
     * Number of failed sync attempts
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    /**
     * Number of conflicts detected
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer conflictCount = 0;

    /**
     * Genesis hash (first transaction in chain)
     * Default: "0000000000000000000000000000000000000000000000000000000000000000"
     */
    @Column(length = 64)
    private String genesisHash;

    /**
     * Current chain head hash (latest transaction)
     */
    @Column(length = 64)
    private String currentHeadHash;

    /**
     * Last chain validation timestamp
     */
    @Column
    private LocalDateTime lastValidatedAt;

    /**
     * Validation error message (if chain invalid)
     */
    @Column(length = 500)
    private String validationError;

    /**
     * Timestamp when chain was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when chain was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Increment pending count
     */
    public void incrementPending() {
        this.pendingCount++;
        this.totalTransactions++;
    }

    /**
     * Decrement pending count (when synced)
     */
    public void decrementPending() {
        if (this.pendingCount > 0) {
            this.pendingCount--;
        }
    }

    /**
     * Mark transaction as synced
     */
    public void markTransactionSynced(String transactionHash) {
        decrementPending();
        this.syncedCount++;
        this.lastSyncedHash = transactionHash;
        this.lastSyncedAt = LocalDateTime.now();
        this.currentHeadHash = transactionHash;
    }

    /**
     * Mark transaction as failed
     */
    public void markTransactionFailed() {
        decrementPending();
        this.failedCount++;
    }

    /**
     * Mark transaction as conflict
     */
    public void markTransactionConflict() {
        decrementPending();
        this.conflictCount++;
    }

    /**
     * Increment conflict count
     */
    public void incrementConflict() {
        this.conflictCount++;
    }

    /**
     * Invalidate chain
     */
    public void invalidateChain(String errorMessage) {
        this.chainValid = false;
        this.validationError = errorMessage;
        this.lastValidatedAt = LocalDateTime.now();
    }

    /**
     * Validate chain
     */
    public void validateChain() {
        this.chainValid = true;
        this.validationError = null;
        this.lastValidatedAt = LocalDateTime.now();
    }

    /**
     * Check if chain needs validation
     */
    public boolean needsValidation() {
        return this.lastValidatedAt == null ||
               this.lastValidatedAt.isBefore(this.updatedAt);
    }

    /**
     * Get genesis hash (default if null)
     */
    public String getGenesisHash() {
        if (this.genesisHash == null) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
        return this.genesisHash;
    }

    /**
     * Check if chain is valid
     */
    public boolean isChainValid() {
        return this.chainValid != null && this.chainValid;
    }
}
