package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.config.AdminInsightsRateLimiter;
import com.fulus.ai.assistant.entity.AdminAuditLog;
import com.fulus.ai.assistant.entity.AdminConversation;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.UserRole;
import com.fulus.ai.assistant.exception.UnauthorizedException;
import com.fulus.ai.assistant.repository.AdminAuditLogRepository;
import com.fulus.ai.assistant.repository.AdminConversationRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Admin Security Service
 *
 * Provides security validation and audit logging for admin insights features:
 * - Admin role validation
 * - Conversation access control
 * - Rate limiting
 * - IP whitelisting
 * - Comprehensive audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSecurityService {

    private final UserRepository userRepository;
    private final AdminConversationRepository conversationRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminInsightsRateLimiter rateLimiter;

    @Value("${admin.insights.ip-whitelist.enabled:false}")
    private boolean ipWhitelistEnabled;

    @Value("${admin.insights.ip-whitelist.allowed-ips:}")
    private String allowedIps;

    /**
     * Validate that a user has ADMIN or SUPER_ADMIN role
     */
    public boolean validateAdminRole(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("User not found for admin validation: {}", userId);
            return false;
        }

        boolean isAdmin = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;

        if (!isAdmin) {
            log.warn("User {} does not have admin role. Current role: {}", userId, user.getRole());
            auditAdminAction(userId, "ADMIN_ROLE_VALIDATION_FAILED", null,
                    "User attempted to access admin endpoint without admin role",
                    null, null, "FAILURE", null);
        }

        return isAdmin;
    }

    /**
     * Validate that an admin has access to a specific conversation
     */
    public boolean validateConversationAccess(UUID adminId, UUID conversationId) {
        AdminConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElse(null);

        if (conversation == null) {
            log.warn("Conversation not found: {}", conversationId);
            return false;
        }

        boolean hasAccess = conversation.getAdminId().equals(adminId);

        if (!hasAccess) {
            log.warn("Admin {} attempted to access conversation {} owned by admin {}",
                    adminId, conversationId, conversation.getAdminId());

            auditAdminAction(adminId, "CONVERSATION_ACCESS_DENIED", conversationId.toString(),
                    "Attempted to access conversation owned by " + conversation.getAdminId(),
                    null, null, "FAILURE", null);
        }

        return hasAccess;
    }

    /**
     * Audit admin actions with comprehensive logging
     */
    @Transactional
    public void auditAdminAction(
            UUID adminId,
            String action,
            String resourceId,
            String details,
            String ipAddress,
            String userAgent,
            String status,
            Long processingTimeMs) {

        try {
            AdminAuditLog auditLog = new AdminAuditLog();
            auditLog.setAdminId(adminId);
            auditLog.setAction(action);
            auditLog.setResourceId(resourceId);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setStatus(status != null ? status : "SUCCESS");
            auditLog.setProcessingTimeMs(processingTimeMs);
            auditLog.setTimestamp(LocalDateTime.now());

            auditLogRepository.save(auditLog);

            log.debug("Audited admin action: {} by admin {} - Status: {}",
                    action, adminId, status);

        } catch (Exception e) {
            log.error("Failed to audit admin action: {} for admin: {}", action, adminId, e);
        }
    }

    /**
     * Check if admin has exceeded rate limit
     */
    public boolean checkRateLimit(UUID adminId) {
        boolean allowed = rateLimiter.allowRequest(adminId);

        if (!allowed) {
            log.warn("Rate limit exceeded for admin: {}", adminId);
            auditAdminAction(adminId, "RATE_LIMIT_EXCEEDED", null,
                    "Admin exceeded rate limit for insights API",
                    null, null, "BLOCKED", null);
        }

        return allowed;
    }

    /**
     * Validate IP address against whitelist
     */
    public boolean validateIpAddress(String ipAddress) {
        // If whitelist is disabled, allow all IPs
        if (!ipWhitelistEnabled) {
            return true;
        }

        // If no IPs configured, allow all
        if (allowedIps == null || allowedIps.trim().isEmpty()) {
            return true;
        }

        List<String> allowedIpList = Arrays.asList(allowedIps.split(","));

        for (String allowedIp : allowedIpList) {
            String trimmedIp = allowedIp.trim();

            // Handle CIDR notation and wildcards
            if (trimmedIp.equals("0.0.0.0/0") || trimmedIp.equals("*")) {
                return true;
            }

            // Exact match
            if (trimmedIp.equals(ipAddress)) {
                return true;
            }

            // Localhost variations
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("::1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                if (trimmedIp.equals("127.0.0.1") || trimmedIp.equals("::1") || trimmedIp.equals("localhost")) {
                    return true;
                }
            }
        }

        log.warn("IP address {} not in whitelist. Allowed IPs: {}", ipAddress, allowedIps);
        return false;
    }

    /**
     * Sanitize message content to prevent injection attacks
     */
    public String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }

        // Remove potential HTML tags
        String sanitized = message.replaceAll("<[^>]*>", "");

        // Remove potential SQL injection patterns
        sanitized = sanitized.replaceAll("(?i)(;\\s*(DROP|DELETE|UPDATE|INSERT|ALTER|CREATE)\\s)", "");

        // Remove excessive special characters that might indicate an attack
        sanitized = sanitized.replaceAll("[<>\"'%;()&+]", "");

        return sanitized;
    }

    /**
     * Validate message is safe and within limits
     */
    public boolean isMessageValid(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        if (message.length() > 2000) {
            return false;
        }

        // Check for suspicious patterns
        String lower = message.toLowerCase();
        if (lower.contains("<script") || lower.contains("javascript:") ||
            lower.contains("onerror=") || lower.contains("onload=")) {
            log.warn("Potentially malicious message detected: {}", message.substring(0, Math.min(50, message.length())));
            return false;
        }

        return true;
    }
}

