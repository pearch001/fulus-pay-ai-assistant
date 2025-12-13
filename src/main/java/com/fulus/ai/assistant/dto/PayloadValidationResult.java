package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for payload validation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadValidationResult {

    private boolean valid;
    private UUID transactionId;
    private String transactionHash;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private String decryptedPayload;
    private boolean signatureValid;
    private boolean nonceUnique;
    private boolean timestampValid;
    private boolean amountValid;

    /**
     * Add error
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * Add warning
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Check if has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if has warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Factory method for successful validation
     */
    public static PayloadValidationResult success(UUID transactionId, String transactionHash) {
        return PayloadValidationResult.builder()
                .valid(true)
                .transactionId(transactionId)
                .transactionHash(transactionHash)
                .signatureValid(true)
                .nonceUnique(true)
                .timestampValid(true)
                .amountValid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }
}
