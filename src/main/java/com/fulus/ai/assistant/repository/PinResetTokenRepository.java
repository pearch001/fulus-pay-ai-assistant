package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.PinResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PIN reset tokens
 */
@Repository
public interface PinResetTokenRepository extends JpaRepository<PinResetToken, UUID> {

    /**
     * Find PIN reset token by reset token string
     */
    Optional<PinResetToken> findByResetToken(String resetToken);

    /**
     * Find valid PIN reset token by reset token and OTP
     */
    @Query("SELECT p FROM PinResetToken p WHERE p.resetToken = :resetToken " +
            "AND p.otp = :otp AND p.used = false AND p.expiryDate > :now")
    Optional<PinResetToken> findValidToken(String resetToken, String otp, LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PinResetToken p WHERE p.expiryDate < :now")
    int deleteExpiredTokens(LocalDateTime now);

    /**
     * Invalidate all unused tokens for a user (security measure)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PinResetToken p SET p.used = true WHERE p.userId = :userId AND p.used = false")
    int invalidateAllUserTokens(UUID userId);
}
