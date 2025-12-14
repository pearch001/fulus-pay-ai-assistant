package com.fulus.ai.assistant.service.chat;

import com.fulus.ai.assistant.repository.ChatMessageRepository;
import com.fulus.ai.assistant.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PostgreSQLChatMemoryProvider {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final int maxMessages;
    private final long cacheTtlSeconds;

    // Cache to store active memory instances
    private final Map<UUID, CachedMemory> memoryCache = new ConcurrentHashMap<>();

    public PostgreSQLChatMemoryProvider(
            ChatMessageRepository chatMessageRepository,
            ConversationRepository conversationRepository,
            ConversationService conversationService,
            @Value("${spring.ai.chat.memory.max-messages:20}") int maxMessages,
            @Value("${spring.ai.chat.memory.cache-ttl:3600}") long cacheTtlSeconds) {

        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.maxMessages = maxMessages;
        this.cacheTtlSeconds = cacheTtlSeconds;

        log.info("Initialized PostgreSQLChatMemoryProvider with maxMessages={}, cacheTtl={}s",
                maxMessages, cacheTtlSeconds);
    }

    /**
     * Get or create ChatMemory instance for a user
     *
     * @param userId User identifier (UUID as string)
     * @return PostgreSQLChatMemory instance
     */
    public PostgreSQLChatMemory get(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return get(userUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
    }

    /**
     * Get or create ChatMemory instance for a user
     *
     * @param userId User identifier
     * @return PostgreSQLChatMemory instance
     */
    public PostgreSQLChatMemory get(UUID userId) {
        CachedMemory cached = memoryCache.get(userId);

        // Check if cached entry is still valid
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached memory for user {}", userId);
            return cached.memory;
        }

        // Create new memory instance
        log.info("Creating new memory instance for user {}", userId);
        PostgreSQLChatMemory memory = new PostgreSQLChatMemory(
                userId,
                maxMessages,
                chatMessageRepository,
                conversationRepository,
                conversationService
        );

        // Cache it
        memoryCache.put(userId, new CachedMemory(memory, cacheTtlSeconds));

        return memory;
    }

    /**
     * Get memory instance for a user (alias for get)
     */
    public PostgreSQLChatMemory getMemory(UUID userId) {
        return get(userId);
    }

    /**
     * Clear memory for a specific user (alias for clear)
     */
    public void clearMemory(UUID userId) {
        clear(userId);
    }

    /**
     * Clear memory for a specific user
     */
    public void clear(UUID userId) {
        log.info("Clearing memory cache for user {}", userId);
        CachedMemory cached = memoryCache.remove(userId);

        if (cached != null) {
            // Also clear from database
            cached.memory.clear(cached.memory.getConversationId().toString());
        }
    }

    /**
     * Evict expired entries from cache (scheduled every hour)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void evictExpiredEntries() {
        log.info("Running cache eviction task");

        int evicted = 0;
        for (Map.Entry<UUID, CachedMemory> entry : memoryCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                memoryCache.remove(entry.getKey());
                evicted++;
            }
        }

        if (evicted > 0) {
            log.info("Evicted {} expired cache entries", evicted);
        }

        log.debug("Current cache size: {}", memoryCache.size());
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        long activeEntries = memoryCache.values().stream()
                .filter(cached -> !cached.isExpired())
                .count();

        return Map.of(
                "totalEntries", memoryCache.size(),
                "activeEntries", activeEntries,
                "expiredEntries", memoryCache.size() - activeEntries
        );
    }

    /**
     * Wrapper class to store memory instance with expiration time
     */
    private static class CachedMemory {
        private final PostgreSQLChatMemory memory;
        private final Instant expiresAt;

        public CachedMemory(PostgreSQLChatMemory memory, long ttlSeconds) {
            this.memory = memory;
            this.expiresAt = Instant.now().plusSeconds(ttlSeconds);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
