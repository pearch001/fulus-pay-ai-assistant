package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for device information in auth requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Device model is required")
    private String deviceModel;

    @NotBlank(message = "Device OS is required")
    private String deviceOS;
}
