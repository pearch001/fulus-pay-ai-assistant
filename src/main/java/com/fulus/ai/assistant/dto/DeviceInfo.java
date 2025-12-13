package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for device information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    private UUID id;
    private String deviceId;
    private String deviceName;
    private String status;
    private LocalDateTime registeredAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime revokedAt;
    private String suspensionReason;
    private String revocationReason;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
    private boolean canSync;

    /**
     * Calculate days since last seen
     */
    public Long getDaysSinceLastSeen() {
        if (lastSeenAt == null) {
            return null;
        }
        return java.time.Duration.between(lastSeenAt, LocalDateTime.now()).toDays();
    }
}
