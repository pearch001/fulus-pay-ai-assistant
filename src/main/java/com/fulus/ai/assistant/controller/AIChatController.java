package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.ChatRequest;
import com.fulus.ai.assistant.dto.ChatResponse;
import com.fulus.ai.assistant.service.AIFinancialAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for AI Financial Assistant Chat
 *
 * Endpoints:
 * - POST /api/v1/chat - Send message to AI assistant (blocking)
 * - POST /api/v1/chat/stream - Send message and stream response (SSE)
 * - GET /api/v1/chat/stream/{userId} - Stream response via GET (SSE)
 * - DELETE /api/v1/chat/history/{userId} - Clear conversation history
 * - GET /api/v1/chat/history/{userId} - Get conversation history (debug)
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    private final AIFinancialAssistantService aiAssistantService;

    /**
     * Send a message to the AI assistant
     *
     * @param request ChatRequest containing userId and message
     * @return ChatResponse with AI's response
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received from user: {}", request.getUserId());
        log.debug("Message: {}", request.getMessage());

        try {
            // Process the query
            String aiResponse = aiAssistantService.processQuery(
                    request.getUserId(),
                    request.getMessage()
            );

            // Get message count if requested
            Integer messageCount = null;
            List<ChatResponse.ConversationMessage> history = null;

            if (Boolean.TRUE.equals(request.getIncludeHistory())) {
                List<Message> messages = aiAssistantService.getConversationHistory(request.getUserId());
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
                    .conversationId(request.getUserId())
                    .messageCount(messageCount)
                    .history(history)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Chat response generated successfully for user: {}", request.getUserId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error in chat request: {}", e.getMessage());
            ChatResponse errorResponse = ChatResponse.failure("Invalid request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error processing chat request for user: {}", request.getUserId(), e);
            ChatResponse errorResponse = ChatResponse.failure(
                    "An error occurred while processing your request. Please try again later."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Clear conversation history for a user
     *
     * @param userId The user ID
     * @return Success or error response
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<ChatResponse> clearHistory(@PathVariable String userId) {
        log.info("Clear history request for user: {}", userId);

        try {
            aiAssistantService.clearConversationHistory(userId);

            ChatResponse response = ChatResponse.builder()
                    .success(true)
                    .message("Conversation history cleared successfully")
                    .conversationId(userId)
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
     * Get conversation history for a user (debug endpoint)
     *
     * @param userId The user ID
     * @return Conversation history
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<ChatResponse> getHistory(@PathVariable String userId) {
        log.info("Get history request for user: {}", userId);

        try {
            List<Message> messages = aiAssistantService.getConversationHistory(userId);

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
                    .conversationId(userId)
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
     * @param request ChatRequest containing userId and message
     * @return Flux of ServerSentEvents with streaming tokens
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Stream chat request received from user: {}", request.getUserId());
        log.debug("Message: {}", request.getMessage());

        return aiAssistantService.streamQueryWithRetry(request.getUserId(), request.getMessage())
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
                .doOnComplete(() -> log.info("Stream completed for user: {}", request.getUserId()))
                .doOnError(error -> log.error("Stream error for user: {}", request.getUserId(), error))
                .onErrorResume(throwable -> {
                    log.error("Error in stream endpoint for user: {}", request.getUserId(), throwable);
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
     * @param userId The user ID
     * @param message The user's message
     * @return Flux of ServerSentEvents with streaming tokens
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChatGet(
            @PathVariable String userId,
            @RequestParam String message) {
        log.info("Stream chat GET request received from user: {}", userId);
        log.debug("Message: {}", message);

        return aiAssistantService.streamQueryWithProgress(userId, message)
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
     * @param request ChatRequest containing userId and message
     * @return Flux of text tokens
     */
    @PostMapping(value = "/stream/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> streamChatPlainText(@Valid @RequestBody ChatRequest request) {
        log.info("Stream text request received from user: {}", request.getUserId());

        return aiAssistantService.streamQueryWithRetry(request.getUserId(), request.getMessage())
                .doOnComplete(() -> log.info("Text stream completed for user: {}", request.getUserId()))
                .doOnError(error -> log.error("Text stream error for user: {}", request.getUserId(), error));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Chat Service is running");
    }
}
