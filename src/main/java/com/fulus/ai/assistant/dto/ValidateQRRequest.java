package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for QR code validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateQRRequest {

    @NotBlank(message = "QR data is required")
    private String qrData;
}
