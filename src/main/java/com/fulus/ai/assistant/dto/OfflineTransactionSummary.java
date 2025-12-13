package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary of offline transactions for AI assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineTransactionSummary {

    private String userId;
    private String syncStatus;
    private Integer transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private LocalDateTime oldestPendingDate;
    private LocalDateTime newestTransactionDate;
    private Integer pendingCount;
    private Integer syncedCount;
    private Integer failedCount;
    private String summary; // Plain English summary for AI

    /**
     * Generate plain English summary
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();

        if (transactionCount == 0) {
            sb.append("You have no offline transactions");
            if (syncStatus != null && !syncStatus.equals("all")) {
                sb.append(" with status '").append(syncStatus).append("'");
            }
            sb.append(".");
            return sb.toString();
        }

        sb.append("You have ").append(transactionCount).append(" offline transaction");
        if (transactionCount > 1) {
            sb.append("s");
        }

        if (syncStatus != null && !syncStatus.equals("all")) {
            sb.append(" with status '").append(syncStatus).append("'");
        }

        sb.append(". Total amount: ₦").append(totalAmount);

        if (totalDebits != null && totalDebits.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" (Debits: ₦").append(totalDebits);
            if (totalCredits != null && totalCredits.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(", Credits: ₦").append(totalCredits);
            }
            sb.append(")");
        }

        if (oldestPendingDate != null) {
            sb.append(". Oldest pending transaction from ").append(oldestPendingDate).append(".");
        }

        if (pendingCount != null && pendingCount > 0) {
            sb.append(" You have ").append(pendingCount).append(" transaction");
            if (pendingCount > 1) {
                sb.append("s");
            }
            sb.append(" waiting to sync.");
        }

        if (failedCount != null && failedCount > 0) {
            sb.append(" ").append(failedCount).append(" transaction");
            if (failedCount > 1) {
                sb.append("s");
            }
            sb.append(" failed to sync.");
        }

        return sb.toString();
    }
}
