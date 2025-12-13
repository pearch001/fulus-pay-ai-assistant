package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.DeviceInfo;
import com.fulus.ai.assistant.dto.DeviceRegistrationResponse;
import com.fulus.ai.assistant.dto.RegisterDeviceRequest;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.entity.UserDevice;
import com.fulus.ai.assistant.enums.DeviceStatus;
import com.fulus.ai.assistant.enums.SyncStatus;
import com.fulus.ai.assistant.repository.OfflineTransactionRepository;
import com.fulus.ai.assistant.repository.UserDeviceRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user devices
 * Handles device registration, suspension, and revocation for security
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final OfflineTransactionRepository offlineTransactionRepository;
    private final CryptoUtilityService cryptoUtilityService;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int MAX_DEVICES_PER_USER = 5;

    /**
     * Register a new device for user
     *
     * @param userId  User ID
     * @param request Device registration request
     * @return Device registration response
     */
    @Transactional
    public DeviceRegistrationResponse registerDevice(String userId, RegisterDeviceRequest request) {
        try {
            log.info("Registering device for user: {}, deviceId: {}", userId, request.getDeviceId());

            UUID userUuid = UUID.fromString(userId);

            // Verify user exists
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Check if device already exists
            if (userDeviceRepository.existsByDeviceId(request.getDeviceId())) {
                UserDevice existingDevice = userDeviceRepository.findByDeviceId(request.getDeviceId())
                        .orElseThrow();

                // Check if device belongs to this user
                if (!existingDevice.getUserId().equals(userUuid)) {
                    throw new IllegalStateException("Device ID already registered to another user");
                }

                // Check device status
                if (existingDevice.getStatus() == DeviceStatus.REVOKED) {
                    throw new IllegalStateException("Device has been revoked. Please use a different device ID.");
                }

                // Update existing device
                existingDevice.setDeviceName(request.getDeviceName());
                existingDevice.setPublicKey(request.getPublicKey());
                existingDevice.setDeviceModel(request.getDeviceModel());
                existingDevice.setOsVersion(request.getOsVersion());
                existingDevice.setAppVersion(request.getAppVersion());
                existingDevice.updateLastSeen();

                // Reactivate if suspended
                if (existingDevice.getStatus() == DeviceStatus.SUSPENDED) {
                    existingDevice.reactivate();
                    log.info("Reactivated suspended device: {}", request.getDeviceId());
                }

                userDeviceRepository.save(existingDevice);

                log.info("Updated existing device: {}", request.getDeviceId());
                return DeviceRegistrationResponse.success(
                        existingDevice.getId(),
                        existingDevice.getDeviceId(),
                        existingDevice.getDeviceName(),
                        existingDevice.getRegisteredAt()
                );
            }

            // Check device limit
            long deviceCount = userDeviceRepository.countActiveDevicesByUserId(userUuid);
            if (deviceCount >= MAX_DEVICES_PER_USER) {
                throw new IllegalStateException(
                        String.format("Maximum device limit reached (%d devices). Please revoke an old device first.",
                                MAX_DEVICES_PER_USER));
            }

            // Create new device
            UserDevice device = UserDevice.create(
                    userUuid,
                    request.getDeviceId(),
                    request.getDeviceName(),
                    request.getPublicKey()
            );

            device.setDeviceModel(request.getDeviceModel());
            device.setOsVersion(request.getOsVersion());
            device.setAppVersion(request.getAppVersion());

            userDeviceRepository.save(device);

            // Store public key in crypto service
            com.fulus.ai.assistant.dto.KeyPairDTO keyPairDTO = com.fulus.ai.assistant.dto.KeyPairDTO.builder()
                    .keyId(request.getDeviceId())
                    .publicKey(request.getPublicKey())
                    .algorithm("RSA") // Default, can be detected from key
                    .keySize(2048)
                    .build();

            cryptoUtilityService.storePublicKey(userUuid, keyPairDTO);

            log.info("Device registered successfully: userId={}, deviceId={}", userId, request.getDeviceId());

            return DeviceRegistrationResponse.success(
                    device.getId(),
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getRegisteredAt()
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Device registration failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error registering device", e);
            throw new RuntimeException("Failed to register device: " + e.getMessage(), e);
        }
    }

    /**
     * List all devices for user
     *
     * @param userId User ID
     * @return List of device information
     */
    public List<DeviceInfo> listDevices(String userId) {
        try {
            log.debug("Listing devices for user: {}", userId);

            UUID userUuid = UUID.fromString(userId);

            List<UserDevice> devices = userDeviceRepository.findByUserIdOrderByRegisteredAtDesc(userUuid);

            return devices.stream()
                    .map(this::convertToDeviceInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error listing devices for user: {}", userId, e);
            throw new RuntimeException("Failed to list devices: " + e.getMessage(), e);
        }
    }

    /**
     * Revoke a device (requires PIN confirmation)
     *
     * @param userId   User ID
     * @param deviceId Device ID to revoke
     * @param pin      User's PIN for confirmation
     * @param reason   Optional reason for revocation
     */
    @Transactional
    public void revokeDevice(String userId, String deviceId, String pin, String reason) {
        try {
            log.info("Revoking device: userId={}, deviceId={}", userId, deviceId);

            UUID userUuid = UUID.fromString(userId);

            // Verify user and PIN
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            if (!passwordEncoder.matches(pin, user.getPin())) {
                log.warn("SECURITY ALERT: Invalid PIN attempt for device revocation: userId={}", userId);
                throw new IllegalArgumentException("Invalid PIN");
            }

            // Find device
            UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(userUuid, deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            if (device.getStatus() == DeviceStatus.REVOKED) {
                throw new IllegalStateException("Device is already revoked");
            }

            // Revoke device
            device.revoke(reason != null ? reason : "Revoked by user");
            userDeviceRepository.save(device);

            // Invalidate all pending offline transactions from this device
            invalidatePendingTransactions(user.getPhoneNumber(), deviceId);

            log.info("Device revoked successfully: deviceId={}", deviceId);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Device revocation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error revoking device", e);
            throw new RuntimeException("Failed to revoke device: " + e.getMessage(), e);
        }
    }

    /**
     * Suspend a device temporarily
     *
     * @param userId   User ID
     * @param deviceId Device ID to suspend
     * @param reason   Optional reason for suspension
     */
    @Transactional
    public void suspendDevice(String userId, String deviceId, String reason) {
        try {
            log.info("Suspending device: userId={}, deviceId={}", userId, deviceId);

            UUID userUuid = UUID.fromString(userId);

            // Find device
            UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(userUuid, deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            if (device.getStatus() == DeviceStatus.REVOKED) {
                throw new IllegalStateException("Cannot suspend a revoked device");
            }

            if (device.getStatus() == DeviceStatus.SUSPENDED) {
                throw new IllegalStateException("Device is already suspended");
            }

            // Suspend device
            device.suspend(reason != null ? reason : "Suspended by user");
            userDeviceRepository.save(device);

            log.info("Device suspended successfully: deviceId={}", deviceId);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Device suspension failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error suspending device", e);
            throw new RuntimeException("Failed to suspend device: " + e.getMessage(), e);
        }
    }

    /**
     * Reactivate a suspended device
     *
     * @param userId   User ID
     * @param deviceId Device ID to reactivate
     */
    @Transactional
    public void reactivateDevice(String userId, String deviceId) {
        try {
            log.info("Reactivating device: userId={}, deviceId={}", userId, deviceId);

            UUID userUuid = UUID.fromString(userId);

            // Find device
            UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(userUuid, deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            if (device.getStatus() == DeviceStatus.REVOKED) {
                throw new IllegalStateException("Cannot reactivate a revoked device");
            }

            if (device.getStatus() == DeviceStatus.ACTIVE) {
                throw new IllegalStateException("Device is already active");
            }

            // Reactivate device
            device.reactivate();
            userDeviceRepository.save(device);

            log.info("Device reactivated successfully: deviceId={}", deviceId);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Device reactivation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error reactivating device", e);
            throw new RuntimeException("Failed to reactivate device: " + e.getMessage(), e);
        }
    }

    /**
     * Check if device can sync transactions
     *
     * @param deviceId Device ID
     * @return true if device can sync, false otherwise
     */
    public boolean canDeviceSync(String deviceId) {
        return userDeviceRepository.findByDeviceId(deviceId)
                .map(UserDevice::canSync)
                .orElse(false);
    }

    /**
     * Update device last seen timestamp
     *
     * @param deviceId Device ID
     */
    @Transactional
    public void updateDeviceLastSeen(String deviceId) {
        userDeviceRepository.findByDeviceId(deviceId)
                .ifPresent(device -> {
                    device.updateLastSeen();
                    userDeviceRepository.save(device);
                    log.debug("Updated last seen for device: {}", deviceId);
                });
    }

    /**
     * Get device statistics for user
     *
     * @param userId User ID
     * @return Statistics map
     */
    public java.util.Map<String, Object> getDeviceStatistics(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);

            long totalDevices = userDeviceRepository.countByUserId(userUuid);
            long activeDevices = userDeviceRepository.countActiveDevicesByUserId(userUuid);
            long suspendedDevices = userDeviceRepository.findByUserIdAndStatus(userUuid, DeviceStatus.SUSPENDED).size();
            long revokedDevices = userDeviceRepository.findByUserIdAndStatus(userUuid, DeviceStatus.REVOKED).size();

            return java.util.Map.of(
                    "totalDevices", totalDevices,
                    "activeDevices", activeDevices,
                    "suspendedDevices", suspendedDevices,
                    "revokedDevices", revokedDevices,
                    "maxDevicesAllowed", MAX_DEVICES_PER_USER
            );

        } catch (Exception e) {
            log.error("Error getting device statistics for user: {}", userId, e);
            return java.util.Map.of();
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Convert UserDevice entity to DeviceInfo DTO
     */
    private DeviceInfo convertToDeviceInfo(UserDevice device) {
        return DeviceInfo.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .status(device.getStatus().name())
                .registeredAt(device.getRegisteredAt())
                .lastSeenAt(device.getLastSeenAt())
                .suspendedAt(device.getSuspendedAt())
                .revokedAt(device.getRevokedAt())
                .suspensionReason(device.getSuspensionReason())
                .revocationReason(device.getRevocationReason())
                .deviceModel(device.getDeviceModel())
                .osVersion(device.getOsVersion())
                .appVersion(device.getAppVersion())
                .canSync(device.canSync())
                .build();
    }

    /**
     * Invalidate all pending offline transactions from a revoked device
     */
    private void invalidatePendingTransactions(String userPhoneNumber, String deviceId) {
        try {
            log.info("Invalidating pending transactions from device: {}", deviceId);

            // Find all pending transactions from this user
            List<OfflineTransaction> pendingTransactions = offlineTransactionRepository
                    .findByUserPhoneNumberAndSyncStatus(userPhoneNumber, SyncStatus.PENDING);

            // Mark them as failed
            int invalidatedCount = 0;
            for (OfflineTransaction transaction : pendingTransactions) {
                transaction.markAsFailed("Device revoked - transaction invalidated");
                offlineTransactionRepository.save(transaction);
                invalidatedCount++;
            }

            log.info("Invalidated {} pending transactions from device: {}", invalidatedCount, deviceId);

        } catch (Exception e) {
            log.error("Error invalidating transactions for device: {}", deviceId, e);
        }
    }
}
