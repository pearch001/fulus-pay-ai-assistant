package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for offline transactions with blockchain-like features
 *
 * Supports offline peer-to-peer transactions with:
 * - Hash chain integrity
 * - Digital signatures
 * - Replay attack prevention (nonce)
 * - Conflict detection and resolution
 */
@Entity
@Table(name = "offline_transactions", indexes = {
    @Index(name = "idx_sender_phone", columnList = "senderPhoneNumber"),
    @Index(name = "idx_recipient_phone", columnList = "recipientPhoneNumber"),
    @Index(name = "idx_transaction_hash", columnList = "transactionHash", unique = true),
    @Index(name = "idx_nonce", columnList = "nonce", unique = true),
    @Index(name = "idx_sync_status", columnList = "syncStatus"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Sender's phone number (11-digit Nigerian format)
     */
    @Column(nullable = false, length = 20)
    private String senderPhoneNumber;

    /**
     * Recipient's phone number (11-digit Nigerian format)
     */
    @Column(nullable = false, length = 20)
    private String recipientPhoneNumber;

    /**
     * Transaction amount in Naira
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * SHA-256 hash of transaction payload
     * Calculated as: SHA256(senderPhone + recipientPhone + amount + timestamp + nonce + previousHash)
     */
    @Column(nullable = false, length = 64, unique = true)
    private String transactionHash;

    /**
     * Hash of previous transaction in the chain (for chain integrity)
     * First transaction in chain will have previousHash = "0000000000000000..."
     */
    @Column(nullable = false, length = 64)
    private String previousHash;

    /**
     * Encrypted JSON payload containing full transaction details
     * Structure: {
     *   "senderPhone": "08012345678",
     *   "recipientPhone": "08087654321",
     *   "amount": 1000.00,
     *   "description": "Payment for goods",
     *   "metadata": {...}
     * }
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Digital signature of the transaction (sender's signature)
     * Used to verify transaction authenticity
     * Format: Base64 encoded signature
     */
    @Column(nullable = false, length = 500)
    private String signatureKey;

    /**
     * Transaction timestamp (when created offline)
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Sync status (PENDING, SYNCED, FAILED, CONFLICT)
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    /**
     * Nonce (number used once) - prevents replay attacks
     * Unique identifier generated for each transaction
     * Format: UUID or random 32-character hex string
     */
    @Column(nullable = false, length = 64, unique = true)
    private String nonce;

    /**
     * Description/note for the transaction
     */
    @Column(length = 500)
    private String description;

    /**
     * Unique transaction reference for tracking
     * Format: OFFLINE-{timestamp}-{hash_prefix}
     */
    @Column(unique = true, length = 100)
    private String reference;

    /**
     * Offline balance before transaction (sender's perspective)
     * Used for offline balance validation
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal senderOfflineBalance;

    /**
     * Number of sync attempts
     */
    @Column
    @Builder.Default
    private Integer syncAttempts = 0;

    /**
     * Last sync attempt timestamp
     */
    @Column
    private LocalDateTime lastSyncAttempt;

    /**
     * Error message from last sync attempt (if failed)
     */
    @Column(length = 500)
    private String syncError;

    /**
     * Timestamp when transaction was created in database
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when transaction was successfully synced
     */
    @Column
    private LocalDateTime syncedAt;

    /**
     * ID of the online transaction created after successful sync
     */
    @Column
    private UUID onlineTransactionId;

    /**
     * Check if transaction is pending sync
     */
    public boolean isPending() {
        return this.syncStatus == SyncStatus.PENDING;
    }

    /**
     * Check if transaction is synced
     */
    public boolean isSynced() {
        return this.syncStatus == SyncStatus.SYNCED;
    }

    /**
     * Check if transaction has conflict
     */
    public boolean hasConflict() {
        return this.syncStatus == SyncStatus.CONFLICT;
    }

    /**
     * Mark as synced
     */
    public void markAsSynced(UUID onlineTransactionId) {
        this.syncStatus = SyncStatus.SYNCED;
        this.syncedAt = LocalDateTime.now();
        this.onlineTransactionId = onlineTransactionId;
        this.syncError = null;
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String errorMessage) {
        this.syncStatus = SyncStatus.FAILED;
        this.lastSyncAttempt = LocalDateTime.now();
        this.syncAttempts++;
        this.syncError = errorMessage;
    }

    /**
     * Mark as conflict
     */
    public void markAsConflict(String errorMessage) {
        this.syncStatus = SyncStatus.CONFLICT;
        this.lastSyncAttempt = LocalDateTime.now();
        this.syncError = errorMessage;
    }

    /**
     * Retry sync
     */
    public void retrySync() {
        this.syncStatus = SyncStatus.PENDING;
        this.lastSyncAttempt = LocalDateTime.now();
        this.syncAttempts++;
    }
}
