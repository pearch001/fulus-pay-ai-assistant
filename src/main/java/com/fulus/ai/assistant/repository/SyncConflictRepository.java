package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.SyncConflict;
import com.fulus.ai.assistant.enums.ConflictType;
import com.fulus.ai.assistant.enums.ResolutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for sync conflicts
 */
@Repository
public interface SyncConflictRepository extends JpaRepository<SyncConflict, UUID> {

    /**
     * Find conflicts by transaction ID
     */
    List<SyncConflict> findByTransactionId(UUID transactionId);

    /**
     * Find conflicts by transaction ID ordered by detection date (most recent first)
     */
    List<SyncConflict> findByTransactionIdOrderByDetectedAtDesc(UUID transactionId);

    /**
     * Find unresolved conflicts for a user
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.userId = :userId " +
           "AND c.resolutionStatus IN ('UNRESOLVED', 'PENDING_USER') " +
           "ORDER BY c.priority DESC, c.detectedAt ASC")
    List<SyncConflict> findUnresolvedConflictsByUser(@Param("userId") UUID userId);

    /**
     * Find all unresolved conflicts
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.resolutionStatus IN ('UNRESOLVED', 'PENDING_USER') " +
           "ORDER BY c.priority DESC, c.detectedAt ASC")
    List<SyncConflict> findAllUnresolvedConflicts();

    /**
     * Find conflicts by type
     */
    List<SyncConflict> findByConflictType(ConflictType conflictType);

    /**
     * Find conflicts by resolution status
     */
    List<SyncConflict> findByResolutionStatus(ResolutionStatus resolutionStatus);

    /**
     * Find critical unresolved conflicts (priority >= 4)
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.resolutionStatus IN ('UNRESOLVED', 'PENDING_USER') " +
           "AND c.priority >= 4 " +
           "ORDER BY c.priority DESC, c.detectedAt ASC")
    List<SyncConflict> findCriticalUnresolvedConflicts();

    /**
     * Count unresolved conflicts by user
     */
    @Query("SELECT COUNT(c) FROM SyncConflict c WHERE " +
           "c.userId = :userId " +
           "AND c.resolutionStatus IN ('UNRESOLVED', 'PENDING_USER')")
    Long countUnresolvedConflictsByUser(@Param("userId") UUID userId);

    /**
     * Count conflicts by type
     */
    @Query("SELECT c.conflictType, COUNT(c) FROM SyncConflict c " +
           "GROUP BY c.conflictType " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> countConflictsByType();

    /**
     * Find conflicts for a user by date range
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.userId = :userId " +
           "AND c.detectedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.detectedAt DESC")
    List<SyncConflict> findConflictsByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find old unresolved conflicts (for escalation)
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.resolutionStatus = 'UNRESOLVED' " +
           "AND c.detectedAt < :threshold " +
           "ORDER BY c.priority DESC, c.detectedAt ASC")
    List<SyncConflict> findOldUnresolvedConflicts(@Param("threshold") LocalDateTime threshold);

    /**
     * Find conflicts that failed auto-resolution
     */
    @Query("SELECT c FROM SyncConflict c WHERE " +
           "c.autoResolutionAttempted = true " +
           "AND c.resolutionStatus = 'UNRESOLVED' " +
           "ORDER BY c.priority DESC, c.detectedAt ASC")
    List<SyncConflict> findFailedAutoResolutions();

    /**
     * Get conflict statistics
     */
    @Query("SELECT " +
           "COUNT(c) as totalConflicts, " +
           "COUNT(CASE WHEN c.resolutionStatus IN ('UNRESOLVED', 'PENDING_USER') THEN 1 END) as unresolved, " +
           "COUNT(CASE WHEN c.resolutionStatus = 'AUTO_RESOLVED' THEN 1 END) as autoResolved, " +
           "COUNT(CASE WHEN c.resolutionStatus = 'MANUAL_RESOLVED' THEN 1 END) as manualResolved, " +
           "COUNT(CASE WHEN c.resolutionStatus = 'REJECTED' THEN 1 END) as rejected, " +
           "COUNT(CASE WHEN c.priority >= 4 THEN 1 END) as critical " +
           "FROM SyncConflict c")
    Object[] getConflictStatistics();

    /**
     * Find most common conflict type
     */
    @Query("SELECT c.conflictType FROM SyncConflict c " +
           "GROUP BY c.conflictType " +
           "ORDER BY COUNT(c) DESC " +
           "LIMIT 1")
    Optional<ConflictType> findMostCommonConflictType();

    /**
     * Find conflicts by transaction and type
     */
    Optional<SyncConflict> findByTransactionIdAndConflictType(UUID transactionId, ConflictType conflictType);

    /**
     * Count conflicts by user
     */
    Long countByUserId(UUID userId);

    /**
     * Delete resolved conflicts older than threshold (cleanup)
     */
    @Query("DELETE FROM SyncConflict c WHERE " +
           "c.resolutionStatus IN ('AUTO_RESOLVED', 'MANUAL_RESOLVED', 'REJECTED') " +
           "AND c.resolvedAt < :threshold")
    void deleteOldResolvedConflicts(@Param("threshold") LocalDateTime threshold);
}
