package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.CardStatus;
import com.fulus.ai.assistant.enums.CardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user's debit card (physical or virtual)
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_card_number", columnList = "cardNumber", unique = true),
    @Index(name = "idx_card_type", columnList = "cardType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardType cardType; // PHYSICAL, VIRTUAL

    @Column(nullable = false, unique = true, length = 16)
    private String cardNumber; // Masked: 5399********1234

    @Column(nullable = false, length = 100)
    private String cardHolderName;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false, length = 3)
    private String cvv; // Encrypted/masked in production

    @Column(nullable = false, length = 255)
    private String cardPin; // Hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("500000.00"); // ₦500,000

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal transactionLimit = new BigDecimal("100000.00"); // ₦100,000 per transaction

    @Column(nullable = false)
    private Boolean smsAlertEnabled = false;

    @Column(nullable = false)
    private Boolean internationalTransactionsEnabled = false;

    @Column(nullable = false)
    private Boolean onlineTransactionsEnabled = true;

    @Column(nullable = false)
    private Boolean contactlessEnabled = true;

    // For physical cards
    @Column(length = 255)
    private String deliveryAddress;

    @Column
    private LocalDateTime deliveredAt;

    @Column
    private LocalDateTime activatedAt;

    @Column
    private LocalDateTime blockedAt;

    @Column(length = 500)
    private String blockReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get masked card number (first 4 and last 4 digits visible)
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****-****-****-****";
        }
        return cardNumber.substring(0, 4) + " **** **** " + cardNumber.substring(12);
    }

    /**
     * Check if card is active
     */
    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    /**
     * Check if card is blocked
     */
    public boolean isBlocked() {
        return status == CardStatus.BLOCKED;
    }

    /**
     * Block the card
     */
    public void block(String reason) {
        this.status = CardStatus.BLOCKED;
        this.blockedAt = LocalDateTime.now();
        this.blockReason = reason;
    }

    /**
     * Unblock the card
     */
    public void unblock() {
        this.status = CardStatus.ACTIVE;
        this.blockedAt = null;
        this.blockReason = null;
    }

    /**
     * Activate card (for physical cards after delivery)
     */
    public void activate() {
        this.status = CardStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }
}
