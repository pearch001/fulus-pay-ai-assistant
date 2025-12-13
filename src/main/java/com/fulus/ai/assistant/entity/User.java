package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.enums.KycStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_phone_number", columnList = "phoneNumber"),
    @Index(name = "idx_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = true, length = 255)
    private String pin; // Numeric PIN stored hashed when user creates it

    @Column(length = 11)
    private String bvn;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String residentialAddress;

    @Column(unique = true, length = 20)
    private String accountNumber; // Virtual account number for wallet

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus = KycStatus.PENDING; // KYC verification status

    @Column(length = 500)
    private String kycDocumentUrl; // URL/path to uploaded KYC document

    @Column(length = 50)
    private String kycDocumentType; // NIN, DRIVER_LICENSE, etc.

    @Column
    private LocalDateTime kycVerifiedAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column
    private LocalDateTime lastLoginAt;

    // Device Management Fields
    @Column(length = 255)
    private String deviceId;

    @Column(length = 100)
    private String deviceName;

    @Column(length = 50)
    private String deviceModel;

    @Column(length = 50)
    private String deviceOS;

    @Column
    private LocalDateTime deviceRegisteredAt;

    @Column
    private boolean deviceActive = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods for account locking
    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isAccountLocked() {
        if (lockedUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    public String getFullName() {
        return this.name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setVerified(boolean verified) {
        if (verified) {
            this.kycStatus = KycStatus.VERIFIED;
            this.kycVerifiedAt = LocalDateTime.now();
        } else {
            this.kycStatus = KycStatus.PENDING;
            this.kycVerifiedAt = null;
        }
    }

    // Device management helper methods
    public void registerDevice(String deviceId, String deviceName, String deviceModel, String deviceOS) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.deviceOS = deviceOS;
        this.deviceRegisteredAt = LocalDateTime.now();
        this.deviceActive = true;
    }

    public void deactivateDevice() {
        this.deviceActive = false;
    }

    public boolean hasActiveDevice() {
        return this.deviceActive && this.deviceId != null;
    }

    public boolean isDeviceMatching(String deviceId) {
        return this.deviceId != null && this.deviceId.equals(deviceId);
    }
}
