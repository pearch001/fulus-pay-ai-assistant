package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.UsedNonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for used nonces
 */
@Repository
public interface UsedNonceRepository extends JpaRepository<UsedNonce, UUID> {

    /**
     * Find used nonce by nonce value
     */
    Optional<UsedNonce> findByNonce(String nonce);

    /**
     * Check if nonce exists
     */
    boolean existsByNonce(String nonce);

    /**
     * Check if nonce exists and is not expired
     */
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM UsedNonce n WHERE " +
           "n.nonce = :nonce AND n.expiresAt > :now")
    boolean existsByNonceAndNotExpired(@Param("nonce") String nonce, @Param("now") LocalDateTime now);

    /**
     * Find all nonces used by a user
     */
    List<UsedNonce> findByUserId(UUID userId);

    /**
     * Find expired nonces for cleanup
     */
    @Query("SELECT n FROM UsedNonce n WHERE n.expiresAt < :now")
    List<UsedNonce> findExpiredNonces(@Param("now") LocalDateTime now);

    /**
     * Delete expired nonces
     */
    @Modifying
    @Query("DELETE FROM UsedNonce n WHERE n.expiresAt < :now")
    int deleteExpiredNonces(@Param("now") LocalDateTime now);

    /**
     * Count active nonces for a user
     */
    @Query("SELECT COUNT(n) FROM UsedNonce n WHERE n.userId = :userId AND n.expiresAt > :now")
    Long countActiveNoncesByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find nonces expiring soon (for warning)
     */
    @Query("SELECT n FROM UsedNonce n WHERE n.expiresAt BETWEEN :start AND :end ORDER BY n.expiresAt ASC")
    List<UsedNonce> findNoncesExpiringSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
