package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking used nonces to prevent replay attacks
 * Nonces expire after 7 days
 */
@Entity
@Table(name = "used_nonces", indexes = {
        @Index(name = "idx_nonce", columnList = "nonce", unique = true),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String nonce;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(length = 64)
    private String transactionHash;

    /**
     * Check if nonce has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Factory method to create new used nonce
     */
    public static UsedNonce create(String nonce, UUID userId, String transactionHash) {
        UsedNonce usedNonce = new UsedNonce();
        usedNonce.setNonce(nonce);
        usedNonce.setUserId(userId);
        usedNonce.setUsedAt(LocalDateTime.now());
        usedNonce.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days expiry
        usedNonce.setTransactionHash(transactionHash);
        return usedNonce;
    }
}
