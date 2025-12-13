package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.UserKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user key pairs
 */
@Repository
public interface UserKeyPairRepository extends JpaRepository<UserKeyPair, UUID> {

    /**
     * Find key pair by user ID
     */
    Optional<UserKeyPair> findByUserId(UUID userId);

    /**
     * Find active key pair for user
     */
    @Query("SELECT k FROM UserKeyPair k WHERE k.userId = :userId AND k.active = true " +
           "AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    Optional<UserKeyPair> findActiveKeyPairByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find key pair by key ID
     */
    Optional<UserKeyPair> findByKeyId(String keyId);

    /**
     * Check if user has active key pair
     */
    @Query("SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END FROM UserKeyPair k WHERE " +
           "k.userId = :userId AND k.active = true AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    boolean hasActiveKeyPair(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find all key pairs for a user (including revoked)
     */
    List<UserKeyPair> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find expired key pairs
     */
    @Query("SELECT k FROM UserKeyPair k WHERE k.expiresAt < :now AND k.active = true")
    List<UserKeyPair> findExpiredKeyPairs(@Param("now") LocalDateTime now);

    /**
     * Find key pairs expiring soon
     */
    @Query("SELECT k FROM UserKeyPair k WHERE k.expiresAt BETWEEN :start AND :end AND k.active = true " +
           "ORDER BY k.expiresAt ASC")
    List<UserKeyPair> findKeyPairsExpiringSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count active key pairs by algorithm
     */
    @Query("SELECT k.algorithm, COUNT(k) FROM UserKeyPair k WHERE k.active = true " +
           "AND (k.expiresAt IS NULL OR k.expiresAt > :now) GROUP BY k.algorithm")
    List<Object[]> countActiveKeyPairsByAlgorithm(@Param("now") LocalDateTime now);
}
