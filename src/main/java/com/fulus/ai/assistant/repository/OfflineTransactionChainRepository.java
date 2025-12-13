package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.OfflineTransactionChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for offline transaction chains
 */
@Repository
public interface OfflineTransactionChainRepository extends JpaRepository<OfflineTransactionChain, UUID> {

    /**
     * Find chain by user ID
     */
    Optional<OfflineTransactionChain> findByUserId(UUID userId);

    /**
     * Find chains with pending transactions
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE c.pendingCount > 0 ORDER BY c.pendingCount DESC")
    List<OfflineTransactionChain> findChainsWithPendingTransactions();

    /**
     * Find invalid chains
     */
    List<OfflineTransactionChain> findByChainValid(Boolean chainValid);

    /**
     * Find chains that need validation
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE " +
           "c.lastValidatedAt IS NULL OR " +
           "c.lastValidatedAt < c.updatedAt")
    List<OfflineTransactionChain> findChainsNeedingValidation();

    /**
     * Find chains with high conflict count
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE c.conflictCount > :threshold ORDER BY c.conflictCount DESC")
    List<OfflineTransactionChain> findChainsWithHighConflicts(@Param("threshold") int threshold);

    /**
     * Find chains with failed transactions
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE c.failedCount > 0 ORDER BY c.failedCount DESC")
    List<OfflineTransactionChain> findChainsWithFailures();

    /**
     * Get chain statistics
     */
    @Query("SELECT " +
           "COUNT(c) as totalChains, " +
           "SUM(c.pendingCount) as totalPending, " +
           "SUM(c.syncedCount) as totalSynced, " +
           "SUM(c.failedCount) as totalFailed, " +
           "SUM(c.conflictCount) as totalConflicts, " +
           "COUNT(CASE WHEN c.chainValid = false THEN 1 END) as invalidChains " +
           "FROM OfflineTransactionChain c")
    Object[] getChainStatistics();

    /**
     * Find chains by last sync date range
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE " +
           "c.lastSyncedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.lastSyncedAt DESC")
    List<OfflineTransactionChain> findByLastSyncDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count users with pending offline transactions
     */
    @Query("SELECT COUNT(c) FROM OfflineTransactionChain c WHERE c.pendingCount > 0")
    Long countUsersWithPendingTransactions();

    /**
     * Find chains not synced for a while (for monitoring)
     */
    @Query("SELECT c FROM OfflineTransactionChain c WHERE " +
           "c.pendingCount > 0 " +
           "AND (c.lastSyncedAt IS NULL OR c.lastSyncedAt < :threshold) " +
           "ORDER BY c.lastSyncedAt ASC NULLS FIRST")
    List<OfflineTransactionChain> findStalePendingChains(@Param("threshold") LocalDateTime threshold);

    /**
     * Check if user has a chain
     */
    boolean existsByUserId(UUID userId);
}
