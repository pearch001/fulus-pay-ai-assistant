package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.ConflictType;
import com.fulus.ai.assistant.enums.ResolutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking offline transaction sync conflicts
 */
@Entity
@Table(name = "sync_conflicts", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transactionId"),
    @Index(name = "idx_conflict_type", columnList = "conflictType"),
    @Index(name = "idx_resolution_status", columnList = "resolutionStatus"),
    @Index(name = "idx_user_id_conflict", columnList = "userId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID of the offline transaction that has conflict
     */
    @Column(nullable = false)
    private UUID transactionId;

    /**
     * Transaction hash
     */
    @Column(length = 64)
    private String transactionHash;

    /**
     * Transaction amount
     */
    @Column
    private java.math.BigDecimal transactionAmount;

    /**
     * User ID (for quick lookups)
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * Type of conflict
     */
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConflictType conflictType;

    /**
     * Resolution status
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResolutionStatus resolutionStatus = ResolutionStatus.UNRESOLVED;

    /**
     * Detailed conflict description
     */
    @Column(nullable = false, length = 1000)
    private String conflictDescription;

    /**
     * Suggested resolution action
     */
    @Column(length = 500)
    private String suggestedResolution;

    /**
     * Expected balance at time of transaction
     */
    @Column
    private java.math.BigDecimal expectedBalance;

    /**
     * Actual balance when syncing
     */
    @Column
    private java.math.BigDecimal actualBalance;

    /**
     * Expected value (generic)
     */
    @Column(length = 255)
    private String expectedValue;

    /**
     * Actual value (generic)
     */
    @Column(length = 255)
    private String actualValue;

    /**
     * Expected previous hash (from offline transaction)
     */
    @Column(length = 64)
    private String expectedPreviousHash;

    /**
     * Actual previous hash (from server)
     */
    @Column(length = 64)
    private String actualPreviousHash;

    /**
     * Resolution notes (how it was resolved)
     */
    @Column(length = 1000)
    private String resolutionNotes;

    /**
     * Admin/user who resolved the conflict
     */
    @Column
    private UUID resolvedBy;

    /**
     * Priority level (1=low, 5=critical)
     */
    @Column
    @Builder.Default
    private Integer priority = 3;

    /**
     * Auto-resolution attempted
     */
    @Column
    @Builder.Default
    private Boolean autoResolutionAttempted = false;

    /**
     * Auto-resolution result message
     */
    @Column(length = 500)
    private String autoResolutionResult;

    /**
     * Timestamp when conflict was detected
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    /**
     * Timestamp when conflict was resolved
     */
    @Column
    private LocalDateTime resolvedAt;

    /**
     * Check if conflict is resolved
     */
    public boolean isResolved() {
        return this.resolutionStatus == ResolutionStatus.AUTO_RESOLVED ||
               this.resolutionStatus == ResolutionStatus.MANUAL_RESOLVED ||
               this.resolutionStatus == ResolutionStatus.REJECTED;
    }

    /**
     * Check if conflict is unresolved
     */
    public boolean isUnresolved() {
        return this.resolutionStatus == ResolutionStatus.UNRESOLVED ||
               this.resolutionStatus == ResolutionStatus.PENDING_USER;
    }

    /**
     * Mark as auto-resolved
     */
    public void markAsAutoResolved(String notes) {
        this.resolutionStatus = ResolutionStatus.AUTO_RESOLVED;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Mark as manually resolved
     */
    public void markAsManuallyResolved(UUID resolvedBy, String notes) {
        this.resolutionStatus = ResolutionStatus.MANUAL_RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Mark as rejected
     */
    public void markAsRejected(String reason) {
        this.resolutionStatus = ResolutionStatus.REJECTED;
        this.resolutionNotes = "Rejected: " + reason;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Mark as pending user action
     */
    public void markAsPendingUser(String action) {
        this.resolutionStatus = ResolutionStatus.PENDING_USER;
        this.suggestedResolution = action;
    }

    /**
     * Check if critical priority
     */
    public boolean isCritical() {
        return this.priority != null && this.priority >= 4;
    }

    /**
     * Check if auto resolution was attempted
     */
    public boolean isAutoResolutionAttempted() {
        return this.autoResolutionAttempted != null && this.autoResolutionAttempted;
    }
}
