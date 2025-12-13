package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.UserDevice;
import com.fulus.ai.assistant.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user device management
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    /**
     * Find device by device ID
     */
    Optional<UserDevice> findByDeviceId(String deviceId);

    /**
     * Find device by user ID and device ID
     */
    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Check if device exists
     */
    boolean existsByDeviceId(String deviceId);

    /**
     * Check if device exists for user
     */
    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Find all devices for a user
     */
    List<UserDevice> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    /**
     * Find all active devices for a user
     */
    @Query("SELECT d FROM UserDevice d WHERE d.userId = :userId AND d.status = 'ACTIVE' " +
           "ORDER BY d.lastSeenAt DESC")
    List<UserDevice> findActiveDevicesByUserId(@Param("userId") UUID userId);

    /**
     * Find devices by status
     */
    List<UserDevice> findByStatus(DeviceStatus status);

    /**
     * Find devices by user and status
     */
    List<UserDevice> findByUserIdAndStatus(UUID userId, DeviceStatus status);

    /**
     * Count devices by user
     */
    Long countByUserId(UUID userId);

    /**
     * Count active devices by user
     */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.userId = :userId AND d.status = 'ACTIVE'")
    Long countActiveDevicesByUserId(@Param("userId") UUID userId);

    /**
     * Find devices not seen since a specific date
     */
    @Query("SELECT d FROM UserDevice d WHERE d.lastSeenAt < :since AND d.status = 'ACTIVE' " +
           "ORDER BY d.lastSeenAt ASC")
    List<UserDevice> findInactiveDevicesSince(@Param("since") LocalDateTime since);

    /**
     * Find recently registered devices
     */
    @Query("SELECT d FROM UserDevice d WHERE d.registeredAt > :since ORDER BY d.registeredAt DESC")
    List<UserDevice> findRecentlyRegisteredDevices(@Param("since") LocalDateTime since);

    /**
     * Find suspended devices
     */
    @Query("SELECT d FROM UserDevice d WHERE d.status = 'SUSPENDED' AND d.userId = :userId " +
           "ORDER BY d.suspendedAt DESC")
    List<UserDevice> findSuspendedDevicesByUserId(@Param("userId") UUID userId);

    /**
     * Find revoked devices
     */
    @Query("SELECT d FROM UserDevice d WHERE d.status = 'REVOKED' AND d.userId = :userId " +
           "ORDER BY d.revokedAt DESC")
    List<UserDevice> findRevokedDevicesByUserId(@Param("userId") UUID userId);

    /**
     * Get device statistics
     */
    @Query("SELECT d.status, COUNT(d) FROM UserDevice d GROUP BY d.status")
    List<Object[]> getDeviceStatistics();

    /**
     * Get user device statistics
     */
    @Query("SELECT d.status, COUNT(d) FROM UserDevice d WHERE d.userId = :userId GROUP BY d.status")
    List<Object[]> getUserDeviceStatistics(@Param("userId") UUID userId);
}
