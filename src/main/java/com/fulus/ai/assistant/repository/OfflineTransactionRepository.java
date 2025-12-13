package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for offline transactions with chain validation queries
 */
@Repository
public interface OfflineTransactionRepository extends JpaRepository<OfflineTransaction, UUID> {

    /**
     * Find transaction by hash
     */
    Optional<OfflineTransaction> findByTransactionHash(String transactionHash);

    /**
     * Find transaction by nonce (for replay attack prevention)
     */
    Optional<OfflineTransaction> findByNonce(String nonce);

    /**
     * Find all pending transactions for a user (by phone number)
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "(t.senderPhoneNumber = :phoneNumber OR t.recipientPhoneNumber = :phoneNumber) " +
           "AND t.syncStatus = 'PENDING' " +
           "ORDER BY t.timestamp ASC")
    List<OfflineTransaction> findPendingByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find all pending transactions for sender
     */
    List<OfflineTransaction> findBySenderPhoneNumberAndSyncStatusOrderByTimestampAsc(
            String senderPhoneNumber, SyncStatus syncStatus);

    /**
     * Find all transactions by sync status
     */
    List<OfflineTransaction> findBySyncStatusOrderByTimestampAsc(SyncStatus syncStatus);

    /**
     * Count pending transactions for a phone number
     */
    @Query("SELECT COUNT(t) FROM OfflineTransaction t WHERE " +
           "(t.senderPhoneNumber = :phoneNumber OR t.recipientPhoneNumber = :phoneNumber) " +
           "AND t.syncStatus = 'PENDING'")
    Integer countPendingByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find transactions with conflicts
     */
    List<OfflineTransaction> findBySyncStatus(SyncStatus syncStatus);

    /**
     * Find failed transactions that can be retried
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "t.syncStatus = 'FAILED' " +
           "AND t.syncAttempts < :maxAttempts " +
           "AND (t.lastSyncAttempt IS NULL OR t.lastSyncAttempt < :retryAfter) " +
           "ORDER BY t.timestamp ASC")
    List<OfflineTransaction> findRetryableTransactions(
            @Param("maxAttempts") int maxAttempts,
            @Param("retryAfter") LocalDateTime retryAfter);

    /**
     * Validate chain integrity for a sender
     * Returns transactions with broken chain (previousHash doesn't match)
     */
    @Query(value = "WITH chain AS ( " +
           "  SELECT t.id, t.transaction_hash, t.previous_hash, t.timestamp, " +
           "         LAG(t.transaction_hash) OVER (ORDER BY t.timestamp) as expected_previous_hash " +
           "  FROM offline_transactions t " +
           "  WHERE t.sender_phone_number = :phoneNumber " +
           "  ORDER BY t.timestamp " +
           ") " +
           "SELECT * FROM offline_transactions t " +
           "WHERE t.id IN ( " +
           "  SELECT id FROM chain " +
           "  WHERE previous_hash != COALESCE(expected_previous_hash, '0000000000000000000000000000000000000000000000000000000000000000') " +
           ")",
           nativeQuery = true)
    List<OfflineTransaction> findChainIntegrityViolations(@Param("phoneNumber") String phoneNumber);

    /**
     * Find duplicate transactions (potential double-spend)
     * Same sender, recipient, amount, and similar timestamp (within 5 minutes)
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "t.senderPhoneNumber = :senderPhone " +
           "AND t.recipientPhoneNumber = :recipientPhone " +
           "AND t.amount = :amount " +
           "AND t.timestamp BETWEEN :startTime AND :endTime " +
           "AND t.id != :excludeId")
    List<OfflineTransaction> findPotentialDuplicates(
            @Param("senderPhone") String senderPhoneNumber,
            @Param("recipientPhone") String recipientPhoneNumber,
            @Param("amount") java.math.BigDecimal amount,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * Get chain of transactions for a sender (ordered by timestamp)
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "t.senderPhoneNumber = :phoneNumber " +
           "ORDER BY t.timestamp ASC")
    List<OfflineTransaction> getTransactionChain(@Param("phoneNumber") String phoneNumber);

    /**
     * Find transactions by previous hash
     */
    List<OfflineTransaction> findByPreviousHash(String previousHash);

    /**
     * Get latest synced transaction for a sender
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "t.senderPhoneNumber = :phoneNumber " +
           "AND t.syncStatus = 'SYNCED' " +
           "ORDER BY t.timestamp DESC " +
           "LIMIT 1")
    Optional<OfflineTransaction> findLatestSyncedTransaction(@Param("phoneNumber") String phoneNumber);

    /**
     * Count transactions by status for a phone number
     */
    @Query("SELECT COUNT(t) FROM OfflineTransaction t WHERE " +
           "(t.senderPhoneNumber = :phoneNumber OR t.recipientPhoneNumber = :phoneNumber) " +
           "AND t.syncStatus = :status")
    Long countByPhoneNumberAndStatus(
            @Param("phoneNumber") String phoneNumber,
            @Param("status") SyncStatus status);

    /**
     * Find old pending transactions (for cleanup/warning)
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "t.syncStatus = 'PENDING' " +
           "AND t.timestamp < :threshold " +
           "ORDER BY t.timestamp ASC")
    List<OfflineTransaction> findOldPendingTransactions(@Param("threshold") LocalDateTime threshold);

    /**
     * Check if nonce exists (for replay attack prevention)
     */
    boolean existsByNonce(String nonce);

    /**
     * Check if transaction hash exists
     */
    boolean existsByTransactionHash(String transactionHash);

    /**
     * Count transactions by sync status for a phone number (sender or recipient)
     */
    @Query("SELECT COUNT(t) FROM OfflineTransaction t WHERE " +
           "(t.senderPhoneNumber = :phoneNumber OR t.recipientPhoneNumber = :phoneNumber) " +
           "AND t.syncStatus = :status")
    Integer countByUserPhoneNumberAndSyncStatus(
            @Param("phoneNumber") String phoneNumber,
            @Param("status") SyncStatus status);

    /**
     * Find transactions by phone number and sync status (sender or recipient)
     */
    @Query("SELECT t FROM OfflineTransaction t WHERE " +
           "(t.senderPhoneNumber = :phoneNumber OR t.recipientPhoneNumber = :phoneNumber) " +
           "AND t.syncStatus = :status " +
           "ORDER BY t.timestamp ASC")
    List<OfflineTransaction> findByUserPhoneNumberAndSyncStatus(
            @Param("phoneNumber") String phoneNumber,
            @Param("status") SyncStatus status);
}
