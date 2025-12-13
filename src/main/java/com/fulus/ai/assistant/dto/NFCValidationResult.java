package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of NFC payload validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCValidationResult {

    private boolean valid;
    private boolean signatureValid;
    private boolean hashValid;
    private boolean timestampValid;
    private boolean nonceValid;
    private boolean sizeCompatible;
    private boolean versionSupported;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private NFCPayloadDTO payload;

    /**
     * Add validation error
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * Add validation warning
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Factory method for successful validation
     */
    public static NFCValidationResult success(NFCPayloadDTO payload) {
        return NFCValidationResult.builder()
                .valid(true)
                .signatureValid(true)
                .hashValid(true)
                .timestampValid(true)
                .nonceValid(true)
                .sizeCompatible(true)
                .versionSupported(true)
                .payload(payload)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * Factory method for failed validation
     */
    public static NFCValidationResult failure(String error) {
        NFCValidationResult result = NFCValidationResult.builder()
                .valid(false)
                .signatureValid(false)
                .hashValid(false)
                .timestampValid(false)
                .nonceValid(false)
                .sizeCompatible(false)
                .versionSupported(false)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
        result.addError(error);
        return result;
    }
}
