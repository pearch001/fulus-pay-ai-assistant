package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.ChatRequest;
import com.fulus.ai.assistant.dto.ChatResponse;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.AIFinancialAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for AI Financial Assistant Chat
 *
 * Endpoints:
 * - POST /api/v1/chat - Send message to AI assistant (blocking)
 * - POST /api/v1/chat/stream - Send message and stream response (SSE)
 * - GET /api/v1/chat/stream - Stream response via GET (SSE)
 * - DELETE /api/v1/chat/history - Clear conversation history
 * - GET /api/v1/chat/history - Get conversation history (debug)
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Chat", description = "AI Financial Assistant Chat API")
@SecurityRequirement(name = "Bearer Authentication")
public class AIChatController {

    private final AIFinancialAssistantService aiAssistantService;

    /**
     * Send a message to the AI assistant
     *
     * @param userDetails Authenticated user details from security context
     * @param request ChatRequest containing message
     * @return ChatResponse with AI's response
     */
    @PostMapping
    @Operation(summary = "Chat with AI assistant", description = "Send a message to the AI financial assistant")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("Chat request received from user: {}", userId);
        log.debug("Message: {}", request.getMessage());

        try {
            // Process the query
            String aiResponse = aiAssistantService.processQuery(
                    userId.toString(),
                    request.getMessage()
            );

            // Get message count if requested
            Integer messageCount = null;
            List<ChatResponse.ConversationMessage> history = null;

            if (Boolean.TRUE.equals(request.getIncludeHistory())) {
                List<Message> messages = aiAssistantService.getConversationHistory(userId.toString());
                messageCount = messages.size();

                // Convert to response format
                history = new ArrayList<>();
                for (Message msg : messages) {
                    ChatResponse.ConversationMessage convMsg = ChatResponse.ConversationMessage.builder()
                            .role(msg.getMessageType().getValue())
                            .content(msg.getContent())
                            .timestamp(LocalDateTime.now()) // Message doesn't have timestamp, use current
                            .build();
                    history.add(convMsg);
                }
            }

            // Build success response
            ChatResponse response = ChatResponse.builder()
                    .success(true)
                    .message("Response generated successfully")
                    .response(aiResponse)
                    .conversationId(userId.toString())
                    .messageCount(messageCount)
                    .history(history)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Chat response generated successfully for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error in chat request: {}", e.getMessage());
            ChatResponse errorResponse = ChatResponse.failure("Invalid request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error processing chat request for user: {}", userId, e);
            ChatResponse errorResponse = ChatResponse.failure(
                    "An error occurred while processing your request. Please try again later."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Clear conversation history for authenticated user
     *
     * @param userDetails Authenticated user details from security context
     * @return Success or error response
     */
    @DeleteMapping("/history")
    @Operation(summary = "Clear chat history", description = "Clear conversation history for the authenticated user")
    public ResponseEntity<ChatResponse> clearHistory(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        log.info("Clear history request for user: {}", userId);

        try {
            aiAssistantService.clearConversationHistory(userId.toString());

            ChatResponse response = ChatResponse.builder()
                    .success(true)
                    .message("Conversation history cleared successfully")
                    .conversationId(userId.toString())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Conversation history cleared for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error clearing conversation history for user: {}", userId, e);
            ChatResponse errorResponse = ChatResponse.failure(
                    "Failed to clear conversation history: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get conversation history for authenticated user (debug endpoint)
     *
     * @param userDetails Authenticated user details from security context
     * @return Conversation history
     */
    @GetMapping("/history")
    @Operation(summary = "Get chat history", description = "Retrieve conversation history for the authenticated user")
    public ResponseEntity<ChatResponse> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        log.info("Get history request for user: {}", userId);

        try {
            List<Message> messages = aiAssistantService.getConversationHistory(userId.toString());

            // Convert to response format
            List<ChatResponse.ConversationMessage> history = new ArrayList<>();
            for (Message msg : messages) {
                ChatResponse.ConversationMessage convMsg = ChatResponse.ConversationMessage.builder()
                        .role(msg.getMessageType().getValue())
                        .content(msg.getContent())
                        .timestamp(LocalDateTime.now())
                        .build();
                history.add(convMsg);
            }

            ChatResponse response = ChatResponse.builder()
                    .success(true)
                    .message("Conversation history retrieved successfully")
                    .conversationId(userId.toString())
                    .messageCount(messages.size())
                    .history(history)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Conversation history retrieved for user: {} ({} messages)", userId, messages.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving conversation history for user: {}", userId, e);
            ChatResponse errorResponse = ChatResponse.failure(
                    "Failed to retrieve conversation history: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Stream AI response via Server-Sent Events (SSE)
     * POST endpoint for streaming with request body
     *
     * @param userDetails Authenticated user details from security context
     * @param request ChatRequest containing message
     * @return Flux of ServerSentEvents with streaming tokens
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat response", description = "Stream AI assistant response using Server-Sent Events")
    public Flux<ServerSentEvent<String>> streamChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("Stream chat request received from user: {}", userId);
        log.debug("Message: {}", request.getMessage());

        return aiAssistantService.streamQueryWithRetry(userId.toString(), request.getMessage())
                .map(token -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("message")
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("complete")
                                .data("[DONE]")
                                .build()
                ))
                .doOnComplete(() -> log.info("Stream completed for user: {}", userId))
                .doOnError(error -> log.error("Stream error for user: {}", userId, error))
                .onErrorResume(throwable -> {
                    log.error("Error in stream endpoint for user: {}", userId, throwable);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("❌ An error occurred while streaming the response.")
                                    .build()
                    );
                });
    }

    /**
     * Stream AI response via Server-Sent Events (SSE)
     * GET endpoint for streaming with query parameters
     *
     * @param userDetails Authenticated user details from security context
     * @param message The user's message
     * @return Flux of ServerSentEvents with streaming tokens
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat response (GET)", description = "Stream AI assistant response using GET with query parameters")
    public Flux<ServerSentEvent<String>> streamChatGet(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String message) {

        UUID userId = getUserId(userDetails);
        log.info("Stream chat GET request received from user: {}", userId);
        log.debug("Message: {}", message);

        return aiAssistantService.streamQueryWithProgress(userId.toString(), message)
                .map(token -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("message")
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("complete")
                                .data("[DONE]")
                                .build()
                ))
                .doOnComplete(() -> log.info("Stream completed for user: {}", userId))
                .doOnError(error -> log.error("Stream error for user: {}", userId, error))
                .onErrorResume(throwable -> {
                    log.error("Error in stream GET endpoint for user: {}", userId, throwable);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("❌ An error occurred while streaming the response.")
                                    .build()
                    );
                });
    }

    /**
     * Stream AI response as plain text (alternative to SSE)
     * Useful for simpler clients that don't need SSE format
     *
     * @param userDetails Authenticated user details from security context
     * @param request ChatRequest containing message
     * @return Flux of text tokens
     */
    @PostMapping(value = "/stream/text", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Stream chat response as plain text", description = "Stream AI response as plain text without SSE formatting")
    public Flux<String> streamChatPlainText(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("Stream text request received from user: {}", userId);

        return aiAssistantService.streamQueryWithRetry(userId.toString(), request.getMessage())
                .doOnComplete(() -> log.info("Text stream completed for user: {}", userId))
                .doOnError(error -> log.error("Text stream error for user: {}", userId, error));
    }

    /**
     * Helper method to extract user ID from UserDetails
     */
    private UUID getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal) {
            return ((UserPrincipal) userDetails).getId();
        }
        throw new IllegalStateException("UserDetails is not an instance of UserPrincipal");
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Chat Service is running");
    }
}
