package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for device registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Public key is required")
    private String publicKey;

    private String deviceModel;
    private String osVersion;
    private String appVersion;
}
