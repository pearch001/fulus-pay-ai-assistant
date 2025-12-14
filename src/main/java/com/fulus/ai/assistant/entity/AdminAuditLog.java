package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin Audit Log Entity
 * 
 * Tracks all admin actions for security and compliance
 */
@Entity
@Table(name = "admin_audit_logs", indexes = {
    @Index(name = "idx_audit_admin_id", columnList = "adminId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_resource", columnList = "resourceId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 20)
    private String status; // SUCCESS, FAILURE, ERROR

    @Column
    private Long processingTimeMs;
}

