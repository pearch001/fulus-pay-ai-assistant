package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.config.AdminInsightsRateLimiter;
import com.fulus.ai.assistant.dto.AdminChatRequest;
import com.fulus.ai.assistant.dto.AdminChatResponse;
import com.fulus.ai.assistant.dto.AdminConversationSummary;
import com.fulus.ai.assistant.dto.ApiResponse;
import com.fulus.ai.assistant.entity.AdminChatMessage;
import com.fulus.ai.assistant.entity.AdminConversation;
import com.fulus.ai.assistant.exception.UnauthorizedException;
import com.fulus.ai.assistant.repository.AdminChatMessageRepository;
import com.fulus.ai.assistant.repository.AdminConversationRepository;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.AdminInsightsService;
import com.fulus.ai.assistant.service.AdminSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin Business Insights Controller
 *
 * Provides REST endpoints for admin users to interact with the AI-powered
 * business insights chat system. Includes conversation management, chat
 * interactions, and history retrieval.
 *
 * Security: All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/api/v1/admin/insights")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Insights", description = "Admin business insights and AI chat API")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminInsightsController {

    private final AdminInsightsService adminInsightsService;
    private final AdminConversationRepository conversationRepository;
    private final AdminChatMessageRepository chatMessageRepository;
    private final AdminInsightsRateLimiter rateLimiter;
    private final AdminSecurityService securityService;

    /**
     * POST /api/v1/admin/insights/chat
     *
     * Send a message to the admin business insights AI and receive analysis
     *
     * @param request Chat request with message and optional conversation ID
     * @param userDetails Authenticated user details
     * @return AI-generated business insights response
     */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin insights chat",
               description = "Send message to AI for business insights and analytics")
    public ResponseEntity<AdminChatResponse> chat(
            @Valid @RequestBody AdminChatRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            // 1. Extract admin ID from JWT
            UUID adminId = extractAdminId(userDetails);

            log.info("POST /api/v1/admin/insights/chat - Admin: {}, ConversationId: {}, MessageLength: {}",
                    adminId, request.getConversationId(), request.getMessage().length());

            // 2. Check rate limits
            if (!rateLimiter.allowRequest(adminId)) {
                long remainingMinute = rateLimiter.getRemainingMinuteRequests(adminId);
                long remainingHour = rateLimiter.getRemainingHourRequests(adminId);
                log.warn("Rate limit exceeded for admin: {} (Remaining: {}/min, {}/hour)",
                        adminId, remainingMinute, remainingHour);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(AdminChatResponse.builder()
                                .message("Rate limit exceeded. Please try again later.")
                                .conversationId(null)
                                .timestamp(LocalDateTime.now())
                                .processingTimeMs(0L)
                                .build());
            }

            // 3. Validate admin exists and has ADMIN role
            validateAdminAccess(adminId);

            // 4. Sanitize request
            request = sanitizeRequest(request);

            // 5. Process message
            AdminChatResponse response = adminInsightsService.processMessage(request, adminId);

            // 6. Log successful interaction
            log.info("Admin {} sent message to insights AI - ConversationId: {}, ProcessingTime: {}ms, Tokens: {}",
                    adminId, response.getConversationId(), response.getProcessingTimeMs(), response.getTokenCount());

            // 7. Return response
            return ResponseEntity.ok(response);

        } catch (UnauthorizedException e) {
            log.error("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AdminChatResponse.builder()
                            .message("Unauthorized access")
                            .conversationId(null)
                            .timestamp(LocalDateTime.now())
                            .processingTimeMs(0L)
                            .build());

        } catch (Exception e) {
            log.error("Error processing admin chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AdminChatResponse.builder()
                            .message("An error occurred. Please try again.")
                            .conversationId(null)
                            .timestamp(LocalDateTime.now())
                            .processingTimeMs(0L)
                            .build());
        }
    }

    /**
     * GET /api/v1/admin/insights/conversations
     *
     * Retrieve all conversations for the current admin user
     *
     * @param userDetails Authenticated user details
     * @return List of conversation summaries sorted by last updated
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get admin conversations",
               description = "Retrieve all conversations for current admin, sorted by last updated")
    public ResponseEntity<List<AdminConversationSummary>> getConversations(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        UUID adminId = extractAdminId(userDetails);
        log.info("GET /api/v1/admin/insights/conversations - Admin: {}", adminId);

        List<AdminConversation> conversations = conversationRepository
                .findByAdminIdOrderByUpdatedAtDesc(adminId);

        List<AdminConversationSummary> summaries = conversations.stream()
                .map(c -> AdminConversationSummary.builder()
                        .conversationId(c.getConversationId())
                        .messageCount(c.getMessageCount())
                        .lastMessageAt(c.getUpdatedAt())
                        .subject(c.getSubject())
                        .isActive(c.isActive())
                        .totalTokens(c.getTotalTokens())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();

        log.info("Retrieved {} conversations for admin: {}", summaries.size(), adminId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * GET /api/v1/admin/insights/conversations/{conversationId}/history
     *
     * Retrieve full message history for a specific conversation
     *
     * @param conversationId Conversation UUID
     * @param userDetails Authenticated user details
     * @return List of all messages in chronological order
     */
    @GetMapping("/conversations/{conversationId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get conversation history",
               description = "Retrieve all messages for a specific conversation")
    public ResponseEntity<?> getConversationHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            // Extract admin ID from JWT
            UUID adminId = extractAdminId(userDetails);

            log.info("GET /api/v1/admin/insights/conversations/{}/history - Admin: {}",
                    conversationId, adminId);

            // 1. Find conversation
            AdminConversation conversation = conversationRepository
                    .findByConversationId(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

            // 2. Validate ownership
            if (!conversation.getAdminId().equals(adminId)) {
                log.warn("Admin {} attempted to access conversation {} owned by admin {}",
                        adminId, conversationId, conversation.getAdminId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            // 3. Get messages
            List<AdminChatMessage> messages = chatMessageRepository
                    .findByConversationIdOrderBySequenceNumberAsc(conversationId);

            log.info("Retrieved {} messages for conversation: {}", messages.size(), conversationId);

            // 4. Return messages
            return ResponseEntity.ok(messages);

        } catch (IllegalArgumentException e) {
            log.warn("Conversation not found: {}", conversationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error fetching history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch history"));
        }
    }

    /**
     * DELETE /api/v1/admin/insights/conversations/{conversationId}
     *
     * Delete a conversation (soft delete - sets isActive to false)
     *
     * @param conversationId Conversation UUID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete conversation",
               description = "Soft delete conversation (set isActive=false)")
    public ResponseEntity<?> deleteConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        UUID adminId = extractAdminId(userDetails);
        log.info("DELETE /api/v1/admin/insights/conversations/{} - Admin: {}", conversationId, adminId);

        AdminConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!conversation.getAdminId().equals(adminId)) {
            log.warn("Admin {} attempted to delete conversation {} owned by admin {}",
                    adminId, conversationId, conversation.getAdminId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Soft delete
        conversation.setActive(false);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Soft deleted conversation: {} by admin: {}", conversationId, adminId);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted"));
    }

    /**
     * GET /api/v1/admin/insights/health
     *
     * Health check endpoint for the admin insights API
     *
     * @return API health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check admin insights API health status")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("GET /api/v1/admin/insights/health - Health check");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Admin Business Insights");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    // ==================== Helper Methods ====================

    /**
     * Extract admin ID from authenticated UserDetails
     *
     * @param userDetails Authenticated user details from JWT token
     * @return UUID of the admin user
     * @throws UnauthorizedException if user details are invalid or user ID cannot be extracted
     */
    private UUID extractAdminId(org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User details not found in security context");
        }

        // Check if UserDetails is instance of UserPrincipal (custom implementation)
        if (userDetails instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) userDetails;
            UUID adminId = userPrincipal.getId();

            if (adminId == null) {
                throw new UnauthorizedException("Admin ID not found in user principal");
            }

            return adminId;
        }

        // Fallback: If it's not UserPrincipal, log and throw exception
        log.error("UserDetails is not an instance of UserPrincipal: {}", userDetails.getClass().getName());
        throw new UnauthorizedException("Invalid user details type");
    }

    /**
     * Validate that the admin has proper access rights
     *
     * @param adminId UUID of the admin to validate
     * @throws UnauthorizedException if admin doesn't have required access
     */
    private void validateAdminAccess(UUID adminId) {
        if (adminId == null) {
            throw new UnauthorizedException("Admin ID is required");
        }

        // Validate admin role
        if (!securityService.validateAdminRole(adminId)) {
            throw new UnauthorizedException("User does not have admin privileges");
        }

        log.debug("Admin access validated for admin: {}", adminId);
    }

    /**
     * Sanitize and validate chat message
     *
     * @param request Chat request to sanitize
     * @return Sanitized request
     */
    private AdminChatRequest sanitizeRequest(AdminChatRequest request) {
        if (request.getMessage() != null) {
            String sanitized = securityService.sanitizeMessage(request.getMessage());
            request.setMessage(sanitized);
        }
        return request;
    }

    /**
     * Convert AdminConversation entity to summary DTO
     */
    private AdminConversationSummary toSummary(AdminConversation conversation) {
        // Get last message timestamp
        LocalDateTime lastMessageAt = chatMessageRepository
                .findTopByConversationIdOrderBySequenceNumberDesc(conversation.getConversationId())
                .map(AdminChatMessage::getCreatedAt)
                .orElse(conversation.getCreatedAt());

        // Get message preview (first user message)
        String messagePreview = chatMessageRepository
                .findTopByConversationIdOrderBySequenceNumberAsc(conversation.getConversationId())
                .map(msg -> msg.getContent().length() > 100
                        ? msg.getContent().substring(0, 100) + "..."
                        : msg.getContent())
                .orElse(null);

        return AdminConversationSummary.builder()
                .conversationId(conversation.getConversationId())
                .messageCount(conversation.getMessageCount())
                .lastMessageAt(lastMessageAt)
                .subject(conversation.getSubject())
                .isActive(conversation.isActive())
                .totalTokens(conversation.getTotalTokens())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messagePreview(messagePreview)
                .build();
    }
}























