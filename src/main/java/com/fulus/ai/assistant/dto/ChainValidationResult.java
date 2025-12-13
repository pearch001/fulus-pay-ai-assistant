package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for chain validation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainValidationResult {

    private boolean valid;
    private String userId;
    private Integer totalTransactions;
    private Integer validTransactions;
    private Integer invalidTransactions;

    @Builder.Default
    private List<ChainValidationError> errors = new ArrayList<>();

    /**
     * Add validation error
     */
    public void addError(ChainValidationError error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * Check if has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Get error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Factory method for successful validation
     */
    public static ChainValidationResult success(String userId, int transactionCount) {
        return ChainValidationResult.builder()
                .valid(true)
                .userId(userId)
                .totalTransactions(transactionCount)
                .validTransactions(transactionCount)
                .invalidTransactions(0)
                .errors(new ArrayList<>())
                .build();
    }

    /**
     * Factory method for failed validation
     */
    public static ChainValidationResult failure(String userId, int totalCount, List<ChainValidationError> errors) {
        return ChainValidationResult.builder()
                .valid(false)
                .userId(userId)
                .totalTransactions(totalCount)
                .validTransactions(totalCount - errors.size())
                .invalidTransactions(errors.size())
                .errors(errors)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainValidationError {
        private UUID transactionId;
        private String transactionHash;
        private Integer transactionIndex;
        private String errorType;
        private String errorMessage;
        private String expectedValue;
        private String actualValue;
    }
}
