package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for sync conflicts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResponse {

    private UUID conflictId;
    private UUID transactionId;
    private String transactionHash;
    private String conflictType;
    private String conflictDescription;
    private BigDecimal transactionAmount;
    private BigDecimal expectedBalance;
    private BigDecimal actualBalance;
    private String expectedValue;
    private String actualValue;
    private Integer priority;
    private String resolutionStatus;
    private boolean autoResolutionAttempted;
    private String autoResolutionResult;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private String suggestedResolution;

    /**
     * Add suggested resolution based on conflict type
     */
    public void addSuggestedResolution() {
        this.suggestedResolution = switch (conflictType) {
            case "DOUBLE_SPEND" -> "Review transaction and ensure it hasn't been synced already. Contact support if issue persists.";
            case "INSUFFICIENT_FUNDS" -> "Ensure sufficient balance before syncing. Top up account or reduce transaction amount.";
            case "INVALID_SIGNATURE" -> "Transaction signature is invalid. This transaction may have been tampered with. Do not sync.";
            case "NONCE_REUSED" -> "Nonce has been reused (replay attack detected). This transaction should be rejected.";
            case "INVALID_HASH" -> "Transaction hash verification failed. Transaction may be corrupted or tampered with.";
            case "CHAIN_BROKEN" -> "Chain integrity violated. Review transaction sequence and ensure proper ordering.";
            case "TIMESTAMP_INVALID" -> "Transaction timestamp is invalid. Check device clock settings.";
            default -> "Contact support for manual resolution.";
        };
    }
}
