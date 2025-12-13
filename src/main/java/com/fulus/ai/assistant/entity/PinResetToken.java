package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing PIN reset tokens and OTPs
 */
@Entity
@Table(name = "pin_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 100)
    private String resetToken;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if token is still valid
     */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiryDate);
    }

    /**
     * Mark token as used
     */
    public void markAsUsed() {
        this.used = true;
    }
}
