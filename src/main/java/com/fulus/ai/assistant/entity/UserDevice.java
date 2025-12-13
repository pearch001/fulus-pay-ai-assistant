package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for user device management
 * Tracks devices registered for offline transactions
 */
@Entity
@Table(name = "user_devices", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_device_id", columnList = "deviceId", unique = true),
        @Index(name = "idx_user_device", columnList = "userId,deviceId", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String deviceId;

    @Column(nullable = false, length = 255)
    private String deviceName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column
    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column
    private LocalDateTime suspendedAt;

    @Column
    private LocalDateTime revokedAt;

    @Column(length = 500)
    private String suspensionReason;

    @Column(length = 500)
    private String revocationReason;

    @Column
    private String deviceModel;

    @Column
    private String osVersion;

    @Column
    private String appVersion;

    /**
     * Check if device is active
     */
    public boolean isActive() {
        return status == DeviceStatus.ACTIVE;
    }

    /**
     * Check if device can sync transactions
     */
    public boolean canSync() {
        return status == DeviceStatus.ACTIVE;
    }

    /**
     * Suspend device
     */
    public void suspend(String reason) {
        this.status = DeviceStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.suspensionReason = reason;
    }

    /**
     * Revoke device
     */
    public void revoke(String reason) {
        this.status = DeviceStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.revocationReason = reason;
    }

    /**
     * Reactivate suspended device
     */
    public void reactivate() {
        if (this.status == DeviceStatus.SUSPENDED) {
            this.status = DeviceStatus.ACTIVE;
            this.suspendedAt = null;
            this.suspensionReason = null;
        }
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    /**
     * Factory method to create new device
     */
    public static UserDevice create(UUID userId, String deviceId, String deviceName, String publicKey) {
        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setDeviceName(deviceName);
        device.setPublicKey(publicKey);
        device.setStatus(DeviceStatus.ACTIVE);
        device.setRegisteredAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        return device;
    }
}
