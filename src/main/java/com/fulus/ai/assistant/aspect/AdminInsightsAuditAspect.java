package com.fulus.ai.assistant.aspect;

import com.fulus.ai.assistant.dto.AdminChatRequest;
import com.fulus.ai.assistant.dto.AdminChatResponse;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.AdminSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Admin Insights Audit Aspect
 *
 * Automatically logs all admin AI interactions using AOP with structured logging
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInsightsAuditAspect {

    private final AdminSecurityService securityService;

    /**
     * Audit all chat interactions with structured logging
     */
    @Around("execution(* com.fulus.ai.assistant.controller.AdminInsightsController.chat(..))")
    public Object auditChatInteraction(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Extract request details
        UUID adminId = extractAdminId();
        String ipAddress = extractIpAddress();
        String userAgent = extractUserAgent();
        AdminChatRequest request = extractChatRequest(joinPoint);

        // Set up MDC for structured logging
        setupMDC(adminId, ipAddress);

        String messagePreview = request != null && request.getMessage() != null
                ? truncate(request.getMessage(), 100)
                : "N/A";

        // Log sanitized request
        logRequest(request, messagePreview);

        try {
            // Validate IP whitelist before processing
            if (!securityService.validateIpAddress(ipAddress)) {
                log.warn("IP address blocked - IP: {}", ipAddress);
                securityService.auditAdminAction(
                    adminId,
                    "ADMIN_CHAT_BLOCKED",
                    null,
                    "IP address blocked: " + ipAddress,
                    ipAddress,
                    userAgent,
                    "FAILURE",
                    null
                );
                throw new SecurityException("Access denied from IP: " + ipAddress);
            }

            // Proceed with the actual method
            Object result = joinPoint.proceed();

            long processingTime = System.currentTimeMillis() - startTime;

            // Extract response from ResponseEntity if needed
            AdminChatResponse response = null;
            if (result instanceof org.springframework.http.ResponseEntity) {
                org.springframework.http.ResponseEntity<?> responseEntity = (org.springframework.http.ResponseEntity<?>) result;
                if (responseEntity.getBody() instanceof AdminChatResponse) {
                    response = (AdminChatResponse) responseEntity.getBody();
                }
            } else if (result instanceof AdminChatResponse) {
                response = (AdminChatResponse) result;
            }

            UUID conversationId = response != null ? response.getConversationId() : null;

            // Log response details
            logResponse(response, processingTime);

            // Log token usage for cost tracking
            if (response != null && response.getTokenCount() != null) {
                log.info("Token usage - Tokens: {}, ProcessingTime: {}ms, Cost estimate: ${}",
                        response.getTokenCount(),
                        processingTime,
                        estimateCost(response.getTokenCount()));
            }

            securityService.auditAdminAction(
                adminId,
                "ADMIN_CHAT_MESSAGE_SENT",
                conversationId != null ? conversationId.toString() : null,
                "Message: " + messagePreview + " | Response length: " +
                        (response != null && response.getMessage() != null ? response.getMessage().length() : 0),
                ipAddress,
                userAgent,
                "SUCCESS",
                processingTime
            );

            log.info("Chat interaction completed - ConversationId: {}, ProcessingTime: {}ms",
                    conversationId, processingTime);

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            // Log error with context
            log.error("Chat interaction failed - ProcessingTime: {}ms, Error: {}",
                    processingTime, e.getMessage(), e);

            securityService.auditAdminAction(
                adminId,
                "ADMIN_CHAT_ERROR",
                null,
                "Message: " + messagePreview + " | Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                ipAddress,
                userAgent,
                "ERROR",
                processingTime
            );

            throw e;
        } finally {
            // Clean up MDC
            clearMDC();
        }
    }

    /**
     * Audit conversation deletions
     */
    @Around("execution(* com.fulus.ai.assistant.controller.AdminInsightsController.deleteConversation(..))")
    public Object auditConversationDeletion(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID adminId = extractAdminId();
        String ipAddress = extractIpAddress();
        String userAgent = extractUserAgent();

        // Set up MDC
        setupMDC(adminId, ipAddress);

        // Extract conversation ID from method arguments
        UUID conversationId = null;
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof UUID) {
            conversationId = (UUID) args[0];
        }

        try {
            log.info("Deleting conversation - ConversationId: {}", conversationId);

            Object result = joinPoint.proceed();

            securityService.auditAdminAction(
                adminId,
                "ADMIN_CONVERSATION_DELETED",
                conversationId != null ? conversationId.toString() : null,
                "Conversation deleted successfully",
                ipAddress,
                userAgent,
                "SUCCESS",
                null
            );

            log.info("Conversation deleted successfully - ConversationId: {}", conversationId);

            return result;

        } catch (Exception e) {
            log.error("Conversation deletion failed - ConversationId: {}, Error: {}",
                    conversationId, e.getMessage(), e);

            securityService.auditAdminAction(
                adminId,
                "ADMIN_CONVERSATION_DELETE_FAILED",
                conversationId != null ? conversationId.toString() : null,
                "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                ipAddress,
                userAgent,
                "ERROR",
                null
            );

            throw e;
        } finally {
            clearMDC();
        }
    }

    /**
     * Extract admin ID from security context
     */
    private UUID extractAdminId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
        } catch (Exception e) {
            log.error("Failed to extract admin ID from security context", e);
        }
        return null;
    }

    /**
     * Extract IP address from HTTP request
     */
    private String extractIpAddress() {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                // Check for proxy headers first
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.error("Failed to extract IP address", e);
        }
        return "unknown";
    }

    /**
     * Extract user agent from HTTP request
     */
    private String extractUserAgent() {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? truncate(userAgent, 500) : "unknown";
            }
        } catch (Exception e) {
            log.error("Failed to extract user agent", e);
        }
        return "unknown";
    }

    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Extract AdminChatRequest from method arguments
     */
    private AdminChatRequest extractChatRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof AdminChatRequest) {
                return (AdminChatRequest) arg;
            }
        }
        return null;
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    /**
     * Set up MDC for structured logging
     */
    private void setupMDC(UUID adminId, String ipAddress) {
        MDC.put("adminId", adminId != null ? adminId.toString() : "unknown");
        MDC.put("ipAddress", ipAddress);
        MDC.put("requestId", UUID.randomUUID().toString());
    }

    /**
     * Clear MDC after request processing
     */
    private void clearMDC() {
        MDC.clear();
    }

    /**
     * Log sanitized request details
     */
    private void logRequest(AdminChatRequest request, String messagePreview) {
        if (request != null) {
            log.info("Incoming request - Message preview: '{}', ConversationId: {}, IncludeCharts: {}",
                    sanitizeForLogging(messagePreview),
                    request.getConversationId(),
                    request.getIncludeCharts());
        }
    }

    /**
     * Log response details
     */
    private void logResponse(AdminChatResponse response, long processingTime) {
        if (response != null) {
            String responsePreview = response.getMessage() != null
                    ? truncate(response.getMessage(), 100)
                    : "N/A";

            log.info("Response generated - Preview: '{}', ConversationId: {}, Tokens: {}, ProcessingTime: {}ms",
                    sanitizeForLogging(responsePreview),
                    response.getConversationId(),
                    response.getTokenCount(),
                    processingTime);
        }
    }

    /**
     * Estimate cost based on token count (rough estimate for GPT-4 Turbo)
     */
    private double estimateCost(Integer tokenCount) {
        if (tokenCount == null) {
            return 0.0;
        }
        // GPT-4 Turbo: ~$0.01 per 1K input tokens, ~$0.03 per 1K output tokens
        // Using average of $0.02 per 1K tokens for estimation
        return (tokenCount / 1000.0) * 0.02;
    }

    /**
     * Sanitize string for logging (remove sensitive patterns)
     */
    private String sanitizeForLogging(String value) {
        if (value == null) {
            return "null";
        }
        // Remove potential sensitive patterns
        return value.replaceAll("[<>\"';\\\\]", "*");
    }
}

