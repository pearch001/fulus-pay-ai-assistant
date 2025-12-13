package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for device registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationResponse {

    private UUID deviceRecordId;
    private String deviceId;
    private String deviceName;
    private String status;
    private LocalDateTime registeredAt;
    private String message;

    /**
     * Factory method for successful registration
     */
    public static DeviceRegistrationResponse success(UUID id, String deviceId, String deviceName, LocalDateTime registeredAt) {
        return DeviceRegistrationResponse.builder()
                .deviceRecordId(id)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .status("ACTIVE")
                .registeredAt(registeredAt)
                .message("Device registered successfully")
                .build();
    }
}
