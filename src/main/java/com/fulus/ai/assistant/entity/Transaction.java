package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_reference", columnList = "reference"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_created", columnList = "userId, createdAt"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionCategory category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Flag to indicate if this is an offline transaction
     */
    @Column(nullable = false)
    private Boolean isOffline = false;

    /**
     * Reference to the original offline transaction (if applicable)
     */
    @Column
    private UUID offlineTransactionId;

    /**
     * Phone numbers for offline transactions
     */
    @Column(length = 20)
    private String senderPhoneNumber;

    @Column(length = 20)
    private String recipientPhoneNumber;
}
