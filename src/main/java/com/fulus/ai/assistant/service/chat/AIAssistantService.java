package com.fulus.ai.assistant.service.chat;

import com.fulus.ai.assistant.exception.ChatMemoryException;
import com.fulus.ai.assistant.exception.OpenAIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAssistantService {

    private final ChatClient chatClient;
    private final PostgreSQLChatMemoryProvider memoryProvider;

    /**
     * Send a message to AI assistant with conversation memory and function calling support
     *
     * The AI can automatically call registered functions (e.g., queryTransactions) to fetch
     * real-time data when needed. Function calls are transparent to the caller - the AI decides
     * when to use them based on the user's query.
     *
     * Example queries that trigger function calling:
     * - "How much did I spend last month?"
     * - "Show my food expenses for this week"
     * - "What are my total transactions today?"
     *
     * @param userId  User identifier
     * @param message User's message
     * @return AI assistant's response (may include data from function calls)
     */
    @Retryable(
            retryFor = {ResourceAccessException.class, RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String chat(String userId, String message) {
        log.info("Processing chat request from user: {}", userId);

        try {
            // Get or create memory for this user
            PostgreSQLChatMemory memory = memoryProvider.get(userId);

            // Add user message to memory
            UserMessage userMessage = new UserMessage(message);
            memory.add(memory.getConversationId().toString(), List.of(userMessage));

            // Get conversation history
            List<Message> conversationHistory = memory.get(
                    memory.getConversationId().toString(),
                    20  // Get last 20 messages
            );

            log.debug("Retrieved {} messages from conversation history", conversationHistory.size());

            // Call OpenAI with conversation context
            String response = chatClient.prompt()
                    .messages(conversationHistory)
                    .call()
                    .content();

            log.debug("Received response from OpenAI: {} characters", response.length());

            // Add assistant response to memory
            org.springframework.ai.chat.messages.AssistantMessage assistantMessage =
                    new org.springframework.ai.chat.messages.AssistantMessage(response);
            memory.add(memory.getConversationId().toString(), List.of(assistantMessage));

            return response;

        } catch (ResourceAccessException e) {
            log.error("OpenAI API timeout or connection error: {}", e.getMessage());
            throw new OpenAIServiceException("AI service is temporarily unavailable. Please try again.", e);
        } catch (RestClientException e) {
            log.error("OpenAI API error: {}", e.getMessage());
            throw new OpenAIServiceException("Failed to communicate with AI service. Please check your API key.", e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID: {}", e.getMessage());
            throw new OpenAIServiceException("Invalid user identifier", e);
        } catch (Exception e) {
            log.error("Unexpected error in chat service", e);
            throw new OpenAIServiceException("An unexpected error occurred while processing your request", e);
        }
    }

    /**
     * Send a message without memory (stateless)
     */
    @Retryable(
            retryFor = {ResourceAccessException.class, RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String chatStateless(String message) {
        log.info("Processing stateless chat request");

        try {
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            log.debug("Received stateless response: {} characters", response.length());
            return response;

        } catch (Exception e) {
            log.error("Error in stateless chat", e);
            throw new OpenAIServiceException("Failed to process stateless chat request", e);
        }
    }

    /**
     * Clear conversation history for a user
     */
    public void clearConversation(String userId) {
        log.info("Clearing conversation for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);
            memoryProvider.clear(userUuid);
            log.info("Successfully cleared conversation for user: {}", userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new ChatMemoryException("Invalid user identifier", e);
        } catch (Exception e) {
            log.error("Error clearing conversation", e);
            throw new ChatMemoryException("Failed to clear conversation history", e);
        }
    }

    /**
     * Get conversation history for a user
     */
    public List<Message> getConversationHistory(String userId, int limit) {
        log.info("Retrieving conversation history for user: {}", userId);

        try {
            PostgreSQLChatMemory memory = memoryProvider.get(userId);
            return memory.get(memory.getConversationId().toString(), limit);
        } catch (Exception e) {
            log.error("Error retrieving conversation history", e);
            throw new ChatMemoryException("Failed to retrieve conversation history", e);
        }
    }

    /**
     * Recovery method for retryable operations
     */
    @Recover
    public String recoverFromOpenAIFailure(Exception e, String userId, String message) {
        log.error("All retry attempts failed for user {}: {}", userId, e.getMessage());
        return "I'm having trouble connecting to the AI service right now. Please try again in a moment.";
    }

    /**
     * Get memory cache statistics
     */
    public java.util.Map<String, Object> getCacheStats() {
        return memoryProvider.getCacheStats();
    }
}
