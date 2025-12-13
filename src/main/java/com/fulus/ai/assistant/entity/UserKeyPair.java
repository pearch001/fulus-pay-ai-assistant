package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing user's public key for transaction signing
 * Private key is never stored on backend - only on user's device
 */
@Entity
@Table(name = "user_key_pairs", indexes = {
        @Index(name = "idx_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_key_id", columnList = "keyId", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKeyPair {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String keyId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(nullable = false, length = 20)
    private String algorithm; // RSA or ECDSA

    @Column(nullable = false)
    private Integer keySize; // 2048, 4096 for RSA; 256, 384 for ECDSA

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private LocalDateTime revokedAt;

    @Column(length = 500)
    private String revokeReason;

    /**
     * Check if key is valid (active and not expired)
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * Revoke this key
     */
    public void revoke(String reason) {
        this.active = false;
        this.revokedAt = LocalDateTime.now();
        this.revokeReason = reason;
    }

    /**
     * Factory method to create new key pair record
     */
    public static UserKeyPair create(UUID userId, String publicKey, String algorithm, int keySize) {
        UserKeyPair keyPair = new UserKeyPair();
        keyPair.setUserId(userId);
        keyPair.setKeyId(UUID.randomUUID().toString());
        keyPair.setPublicKey(publicKey);
        keyPair.setAlgorithm(algorithm);
        keyPair.setKeySize(keySize);
        keyPair.setCreatedAt(LocalDateTime.now());
        keyPair.setActive(true);
        // Set expiry to 2 years for production
        keyPair.setExpiresAt(LocalDateTime.now().plusYears(2));
        return keyPair;
    }
}
