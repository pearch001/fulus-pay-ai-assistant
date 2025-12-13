package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for chain validation report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReportResponse {

    private String userId;
    private boolean valid;
    private boolean safeToSync;
    private Integer totalTransactions;
    private Integer validTransactions;
    private Integer invalidTransactions;

    @Builder.Default
    private ChainValidationSummary chainValidation = new ChainValidationSummary();

    @Builder.Default
    private List<PayloadValidationSummary> payloadValidations = new ArrayList<>();

    @Builder.Default
    private DoubleSpendingSummary doubleSpendingCheck = new DoubleSpendingSummary();

    private String recommendation;

    /**
     * Chain validation summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainValidationSummary {
        private boolean valid;
        private Integer errorCount;
        @Builder.Default
        private List<String> errors = new ArrayList<>();
    }

    /**
     * Payload validation summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayloadValidationSummary {
        private String transactionHash;
        private boolean valid;
        private boolean signatureValid;
        private boolean nonceUnique;
        private boolean timestampValid;
        private boolean amountValid;
        @Builder.Default
        private List<String> errors = new ArrayList<>();
        @Builder.Default
        private List<String> warnings = new ArrayList<>();
    }

    /**
     * Double spending summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoubleSpendingSummary {
        private boolean detected;
        private Integer flaggedCount;
        private BigDecimal lastKnownBalance;
        private BigDecimal projectedBalance;
        @Builder.Default
        private List<String> flaggedTransactionHashes = new ArrayList<>();
    }

    /**
     * Generate recommendation based on validation results
     */
    public void generateRecommendation() {
        if (valid && safeToSync) {
            this.recommendation = "All validations passed. Safe to sync transactions.";
        } else if (!chainValidation.isValid()) {
            this.recommendation = "Chain validation failed. Do not sync. Review chain integrity issues.";
        } else if (doubleSpendingCheck.isDetected()) {
            this.recommendation = "Double spending detected. Ensure sufficient balance or remove flagged transactions before syncing.";
        } else if (invalidTransactions > 0) {
            this.recommendation = String.format("%d transaction(s) failed validation. Review errors before syncing.", invalidTransactions);
        } else {
            this.recommendation = "Validation completed with warnings. Review issues before proceeding.";
        }
    }
}
