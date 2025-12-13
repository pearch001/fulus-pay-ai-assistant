package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for revoking a device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokeDeviceRequest {

    @NotBlank(message = "PIN is required for device revocation")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;

    private String reason;
}
