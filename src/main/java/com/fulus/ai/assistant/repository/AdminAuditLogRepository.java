package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AdminAuditLog entity
 * Provides methods to query and manage admin audit logs
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    /**
     * Find all audit logs for a specific admin
     *
     * @param adminId The admin user ID
     * @return List of audit logs
     */
    List<AdminAuditLog> findByAdminIdOrderByTimestampDesc(UUID adminId);

    /**
     * Find audit logs by action type
     *
     * @param action The action type
     * @return List of audit logs
     */
    List<AdminAuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs by status
     *
     * @param status The status (SUCCESS, FAILURE, ERROR)
     * @return List of audit logs
     */
    List<AdminAuditLog> findByStatusOrderByTimestampDesc(String status);

    /**
     * Find audit logs by admin ID and status
     *
     * @param adminId The admin user ID
     * @param status The status
     * @return List of audit logs
     */
    List<AdminAuditLog> findByAdminIdAndStatusOrderByTimestampDesc(UUID adminId, String status);

    /**
     * Find recent audit logs for an admin
     *
     * @param adminId The admin user ID
     * @param limit Maximum number of records
     * @return List of recent audit logs
     */
    @Query("SELECT a FROM AdminAuditLog a WHERE a.adminId = :adminId ORDER BY a.timestamp DESC LIMIT :limit")
    List<AdminAuditLog> findRecentByAdminId(@Param("adminId") UUID adminId, @Param("limit") int limit);

    /**
     * Count failed actions by admin in time window
     *
     * @param adminId The admin user ID
     * @param since Time window start
     * @return Count of failed actions
     */
    @Query("SELECT COUNT(a) FROM AdminAuditLog a WHERE a.adminId = :adminId AND a.status = 'FAILURE' AND a.timestamp > :since")
    long countFailedActionsSince(@Param("adminId") UUID adminId, @Param("since") LocalDateTime since);

    /**
     * Delete old audit logs
     *
     * @param before Delete logs before this timestamp
     */
    void deleteByTimestampBefore(LocalDateTime before);

    /**
     * Find audit logs by resource ID
     *
     * @param resourceId The resource ID
     * @return List of audit logs
     */
    List<AdminAuditLog> findByResourceIdOrderByTimestampDesc(String resourceId);
}

