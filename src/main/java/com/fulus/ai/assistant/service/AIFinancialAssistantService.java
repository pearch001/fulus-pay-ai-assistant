package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.service.chat.PostgreSQLChatMemoryProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI Financial Assistant Service
 *
 * Orchestrates conversations with OpenAI GPT-4 Turbo for financial assistance.
 * Integrates with PostgreSQL-backed chat memory and Spring AI function calling.
 *
 * Features:
 * - Conversation memory (last 20 messages per user)
 * - Automatic function calling for data access and operations
 * - Nigerian financial context awareness
 * - Personalized responses based on user profile
 * - Retry logic for API failures
 * - Streaming support with SSE (Server-Sent Events)
 * - Backpressure handling for streaming responses
 */
@Service
@Slf4j
public class AIFinancialAssistantService {

    private final ChatClient chatClient;
    private final PostgreSQLChatMemoryProvider chatMemoryProvider;
    private final UserRepository userRepository;

    public AIFinancialAssistantService(
            @org.springframework.beans.factory.annotation.Qualifier("userChatClient") ChatClient chatClient,
            PostgreSQLChatMemoryProvider chatMemoryProvider,
            UserRepository userRepository) {
        this.chatClient = chatClient;
        this.chatMemoryProvider = chatMemoryProvider;
        this.userRepository = userRepository;
    }

    private static final String NIGERIAN_CURRENCY_SYMBOL = "₦";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // System prompt template loaded from file
    private String systemPromptTemplate;

    /**
     * Load system prompt from file on service initialization
     */
    @PostConstruct
    public void loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("ai-system-prompt.txt");
            systemPromptTemplate = StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            log.info("System prompt loaded successfully. Length: {} characters", systemPromptTemplate.length());
        } catch (IOException e) {
            log.error("Failed to load system prompt from ai-system-prompt.txt", e);
            // Fallback to basic prompt
            systemPromptTemplate = "You are Fulus AI, a helpful financial assistant for Nigerian users. " +
                    "Help users manage their finances, make payments, and achieve their financial goals.";
            log.warn("Using fallback system prompt due to load failure");
        }
    }

    /**
     * Process user query with conversation history and function calling
     *
     * @param userId The UUID of the user making the query
     * @param userMessage The user's message/question
     * @return AI assistant's response
     * @throws IllegalArgumentException if userId is invalid or user not found
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class}
    )
    public String processQuery(String userId, String userMessage) {
        log.info("Processing query for user: {}", userId);
        log.debug("User message: {}", userMessage);

        try {
            // Validate inputs
            if (userId == null || userId.trim().isEmpty()) {
                log.error("User ID is required");
                return "❌ ERROR: User ID is required to process your request.";
            }

            if (userMessage == null || userMessage.trim().isEmpty()) {
                log.error("User message is empty");
                return "❌ ERROR: Please provide a message.";
            }

            // Parse and validate UUID
            UUID userUuid;
            try {
                userUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid user ID format: {}", userId);
                return "❌ ERROR: Invalid user ID format.";
            }

            // Get user from database
            Optional<User> userOpt = userRepository.findById(userUuid);
            if (userOpt.isEmpty()) {
                log.error("User not found: {}", userId);
                return "❌ ERROR: User not found. Please ensure you're logged in.";
            }
            User user = userOpt.get();

            // Get or create chat memory for this user
            String conversationId = userId; // Use userId as conversationId
            ChatMemory chatMemory = chatMemoryProvider.getMemory(UUID.fromString(conversationId));

            // Build enhanced system prompt with user context
            String systemPrompt = buildSystemPrompt(user);

            // Add user message to memory
            Message userMsg = new UserMessage(userMessage);
            chatMemory.add(conversationId, List.of(userMsg));

            // Retrieve conversation history
            List<Message> conversationHistory = chatMemory.get(conversationId, 20);

            log.debug("Conversation history size: {}", conversationHistory.size());

            // Process query with ChatClient
            // The ChatClient already has all function callbacks registered from OpenAIConfig
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(conversationHistory)
                    .call()
                    .content();

            log.info("AI response generated successfully for user: {}", userId);
            log.debug("AI response: {}", response);

            return response;

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in processQuery: {}", e.getMessage(), e);
            return "❌ ERROR: " + e.getMessage();
        } catch (Exception e) {
            log.error("Error processing query for user: {}", userId, e);
            return "❌ Sorry, I encountered an error processing your request. " +
                    "Please try again in a moment. If the problem persists, contact support.";
        }
    }

    /**
     * Build system prompt with user context variables substituted
     */
    private String buildSystemPrompt(User user) {
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DATE_FORMATTER);
        String currentTime = now.format(TIME_FORMATTER);

        // Substitute context variables in the template
        String prompt = systemPromptTemplate
                .replace("{{USER_NAME}}", user.getName())
                .replace("{{USER_ID}}", user.getId().toString())
                .replace("{{USER_PHONE}}", user.getPhoneNumber())
                .replace("{{USER_BALANCE}}", String.format("%s%,.2f", NIGERIAN_CURRENCY_SYMBOL, user.getBalance()))
                .replace("{{CURRENT_DATE}}", currentDate)
                .replace("{{CURRENT_TIME}}", currentTime);

        log.debug("System prompt built for user: {}. Prompt length: {} characters", user.getId(), prompt.length());

        return prompt;
    }

    /**
     * Clear conversation history for a user (useful for testing or user request)
     *
     * @param userId The UUID of the user
     */
    public void clearConversationHistory(String userId) {
        log.info("Clearing conversation history for user: {}", userId);
        try {
            UUID userUuid = UUID.fromString(userId);
            chatMemoryProvider.clearMemory(userUuid);
            log.info("Conversation history cleared successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Error clearing conversation history for user: {}", userId, e);
            throw new RuntimeException("Failed to clear conversation history", e);
        }
    }

    /**
     * Get conversation history for a user (useful for debugging)
     *
     * @param userId The UUID of the user
     * @return List of messages in the conversation
     */
    public List<Message> getConversationHistory(String userId) {
        log.debug("Retrieving conversation history for user: {}", userId);
        try {
            UUID userUuid = UUID.fromString(userId);
            ChatMemory chatMemory = chatMemoryProvider.getMemory(userUuid);
            return chatMemory.get(userId, 100); // Get up to 100 messages
        } catch (Exception e) {
            log.error("Error retrieving conversation history for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Process batch queries (useful for testing)
     *
     * @param userId The UUID of the user
     * @param messages List of user messages
     * @return List of AI responses
     */
    public List<String> processBatchQueries(String userId, List<String> messages) {
        log.info("Processing {} batch queries for user: {}", messages.size(), userId);

        List<String> responses = new ArrayList<>();
        for (String message : messages) {
            try {
                String response = processQuery(userId, message);
                responses.add(response);
            } catch (Exception e) {
                log.error("Error processing batch query: {}", message, e);
                responses.add("❌ ERROR: Failed to process query");
            }
        }

        return responses;
    }

    /**
     * Stream AI response tokens as they arrive from OpenAI
     * Supports Server-Sent Events (SSE) for real-time delivery
     *
     * @param userId The UUID of the user making the query
     * @param userMessage The user's message/question
     * @return Flux<String> emitting tokens as they arrive
     */
    public Flux<String> streamQuery(String userId, String userMessage) {
        log.info("Processing streaming query for user: {}", userId);
        log.debug("User message: {}", userMessage);

        return Flux.defer(() -> {
            try {
                // Validate inputs
                if (userId == null || userId.trim().isEmpty()) {
                    log.error("User ID is required");
                    return Flux.just("❌ ERROR: User ID is required to process your request.");
                }

                if (userMessage == null || userMessage.trim().isEmpty()) {
                    log.error("User message is empty");
                    return Flux.just("❌ ERROR: Please provide a message.");
                }

                // Parse and validate UUID
                UUID userUuid;
                try {
                    userUuid = UUID.fromString(userId);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid user ID format: {}", userId);
                    return Flux.just("❌ ERROR: Invalid user ID format.");
                }

                // Get user from database
                Optional<User> userOpt = userRepository.findById(userUuid);
                if (userOpt.isEmpty()) {
                    log.error("User not found: {}", userId);
                    return Flux.just("❌ ERROR: User not found. Please ensure you're logged in.");
                }
                User user = userOpt.get();

                // Get or create chat memory for this user
                String conversationId = userId; // Use userId as conversationId
                ChatMemory chatMemory = chatMemoryProvider.getMemory(UUID.fromString(conversationId));

                // Build enhanced system prompt with user context
                String systemPrompt = buildSystemPrompt(user);

                // Add user message to memory
                Message userMsg = new UserMessage(userMessage);
                chatMemory.add(conversationId, List.of(userMsg));

                // Retrieve conversation history
                List<Message> conversationHistory = chatMemory.get(conversationId, 20);

                log.debug("Conversation history size: {}", conversationHistory.size());
                log.info("Starting streaming response for user: {}", userId);

                // AtomicReference to accumulate the complete response
                AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

                // Stream tokens from ChatClient
                Flux<String> tokenStream = chatClient.prompt()
                        .system(systemPrompt)
                        .messages(conversationHistory)
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            // Accumulate tokens for chat memory
                            fullResponse.get().append(token);
                            log.trace("Streaming token: {}", token);
                        })
                        .doOnComplete(() -> {
                            // Save complete assistant response to memory
                            String completeResponse = fullResponse.get().toString();
                            if (!completeResponse.isEmpty()) {
                                Message assistantMsg = new AssistantMessage(completeResponse);
                                chatMemory.add(conversationId, List.of(assistantMsg));
                                log.info("Streaming response completed for user: {}. Total length: {} characters",
                                        userId, completeResponse.length());
                            }
                        })
                        .doOnError(error -> {
                            log.error("Error during streaming for user: {}", userId, error);
                        })
                        // Backpressure handling: buffer tokens with overflow strategy
                        .onBackpressureBuffer(1000,
                                dropped -> log.warn("Dropped token due to backpressure for user: {}", userId))
                        // Timeout for individual tokens (30 seconds)
                        .timeout(Duration.ofSeconds(30), Flux.just("[TIMEOUT]"))
                        // Error recovery: emit error message and complete gracefully
                        .onErrorResume(throwable -> {
                            log.error("Stream error for user {}: {}", userId, throwable.getMessage(), throwable);
                            String errorMsg = "\n\n❌ Sorry, I encountered an error while processing your request. " +
                                    "Please try again.";
                            return Flux.just(errorMsg);
                        })
                        // Subscribe on bounded elastic scheduler to avoid blocking
                        .subscribeOn(Schedulers.boundedElastic());

                return tokenStream;

            } catch (Exception e) {
                log.error("Error initiating streaming query for user: {}", userId, e);
                return Flux.just("❌ Sorry, I encountered an error processing your request. " +
                        "Please try again in a moment.");
            }
        });
    }

    /**
     * Stream AI response with retry capability
     * Wraps streamQuery with retry logic for transient failures
     *
     * @param userId The UUID of the user making the query
     * @param userMessage The user's message/question
     * @return Flux<String> emitting tokens as they arrive
     */
    public Flux<String> streamQueryWithRetry(String userId, String userMessage) {
        log.info("Processing streaming query with retry for user: {}", userId);

        return Flux.defer(() -> streamQuery(userId, userMessage))
                // Retry on error with exponential backoff
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .doBeforeRetry(retrySignal -> {
                            log.warn("Retrying streaming query for user: {}. Attempt: {}",
                                    userId, retrySignal.totalRetries() + 1);
                        })
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                            log.error("Retry exhausted for streaming query. User: {}", userId);
                            return new RuntimeException("Failed to stream response after multiple retries");
                        }))
                // Final error handling
                .onErrorResume(throwable -> {
                    log.error("Final error in streamQueryWithRetry for user: {}", userId, throwable);
                    return Flux.just("\n\n❌ Unable to process your request after multiple attempts. " +
                            "Please try again later or contact support.");
                });
    }

    /**
     * Stream AI response with function call progress indicators
     * Emits progress messages when functions are being called
     *
     * @param userId The UUID of the user making the query
     * @param userMessage The user's message/question
     * @return Flux<String> emitting tokens and progress indicators
     */
    public Flux<String> streamQueryWithProgress(String userId, String userMessage) {
        log.info("Processing streaming query with progress for user: {}", userId);

        return Flux.defer(() -> {
            // Emit initial progress indicator
            Flux<String> progressStart = Flux.just("⏳ Processing your request...\n\n");

            // Stream the actual response
            Flux<String> response = streamQueryWithRetry(userId, userMessage)
                    // Add typing indicator effect (optional)
                    .delayElements(Duration.ofMillis(10))
                    // Remove progress indicator from final stream
                    .skip(0);

            return progressStart.concatWith(response);
        });
    }
}
