package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for KYC verification request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationRequest {

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "NIN|DRIVER_LICENSE|INTERNATIONAL_PASSPORT|VOTERS_CARD",
            message = "Document type must be one of: NIN, DRIVER_LICENSE, INTERNATIONAL_PASSPORT, VOTERS_CARD")
    private String documentType;

    @NotBlank(message = "Document number is required")
    private String documentNumber;
}
