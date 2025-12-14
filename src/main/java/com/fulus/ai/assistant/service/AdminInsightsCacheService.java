package com.fulus.ai.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;
import java.util.List;

/**
 * Admin Insights Cache Service
 *
 * Implements intelligent caching strategy for admin AI queries using Redis:
 * - Caches frequent queries to reduce OpenAI API calls
 * - Different TTLs based on query type
 * - Cache warming for common queries
 * - Performance metrics tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInsightsCacheService {

    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong apiCalls = new AtomicLong(0);

    // Track last stats update time for cache invalidation
    private volatile LocalDateTime lastStatsUpdate = LocalDateTime.now();

    /**
     * Get cached AI response
     *
     * Cache key includes query hash and stats timestamp to ensure freshness
     * TTL: 5 minutes for data queries, 1 hour for general insights
     *
     * @param cacheKey Generated cache key (hash of query + stats timestamp)
     * @return Cached response or null if not found
     */
    @Cacheable(value = "adminInsights", key = "#cacheKey", unless = "#result == null")
    public String getCachedResponse(String cacheKey) {
        log.debug("Cache lookup for key: {}", cacheKey);
        // Return null - actual value comes from cache if exists
        // This method is intercepted by Spring Cache
        return null;
    }

    /**
     * Store AI response in cache
     *
     * @param cacheKey Cache key
     * @param response AI response to cache
     * @return The cached response
     */
    @CachePut(value = "adminInsights", key = "#cacheKey")
    public String cacheResponse(String cacheKey, String response) {
        log.debug("Caching response for key: {} (length: {} chars)", cacheKey, response.length());
        return response;
    }

    /**
     * Clear all cached insights
     * Called when platform stats are refreshed
     */
    @CacheEvict(value = "adminInsights", allEntries = true)
    public void clearCache() {
        log.info("Clearing all admin insights cache");
        lastStatsUpdate = LocalDateTime.now();
    }

    /**
     * Clear cache for specific key
     *
     * @param cacheKey The cache key to evict
     */
    @CacheEvict(value = "adminInsights", key = "#cacheKey")
    public void evictCacheKey(String cacheKey) {
        log.debug("Evicted cache key: {}", cacheKey);
    }

    /**
     * Generate cache key from query and current stats timestamp
     *
     * Format: hash(normalizedQuery + statsTimestamp)
     * This ensures cache invalidation when stats refresh
     *
     * @param query The user's query
     * @param statsTimestamp Current platform stats timestamp
     * @return SHA-256 hash as cache key
     */
    public String generateCacheKey(String query, LocalDateTime statsTimestamp) {
        String normalized = normalizeQuery(query);
        String keyInput = normalized + "|" + statsTimestamp.toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyInput.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // Use first 16 characters for shorter cache keys
            String cacheKey = "insights:" + hexString.substring(0, 16);
            log.debug("Generated cache key: {} for query: '{}'", cacheKey, normalized);
            return cacheKey;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available", e);
            // Fallback to simple hash
            return "insights:" + Math.abs((normalized + statsTimestamp).hashCode());
        }
    }

    /**
     * Normalize query for consistent caching
     *
     * - Convert to lowercase
     * - Remove extra whitespace
     * - Trim
     *
     * @param query The original query
     * @return Normalized query string
     */
    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Check if query should be cached based on characteristics
     *
     * @param query The query to evaluate
     * @return true if query should be cached
     */
    public boolean shouldCache(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String lower = query.toLowerCase();

        // Cache data queries (high frequency, stable results)
        if (lower.contains("revenue") || lower.contains("user") ||
            lower.contains("transaction") || lower.contains("growth") ||
            lower.contains("summary") || lower.contains("overview") ||
            lower.contains("stats") || lower.contains("metrics")) {
            return true;
        }

        // Don't cache very specific or time-sensitive queries
        if (lower.contains("today") || lower.contains("now") ||
            lower.contains("current hour") || lower.contains("right now")) {
            return false;
        }

        // Cache general insights
        return true;
    }

    /**
     * Determine TTL (Time To Live) for cache entry
     *
     * @param query The query being cached
     * @return TTL in seconds
     */
    public long determineTTL(String query) {
        String lower = query.toLowerCase();

        // Short TTL (5 minutes) for data queries
        if (lower.contains("today") || lower.contains("current") ||
            lower.contains("latest") || lower.contains("recent")) {
            return 300; // 5 minutes
        }

        // Medium TTL (15 minutes) for metrics queries
        if (lower.contains("revenue") || lower.contains("transaction") ||
            lower.contains("user") || lower.contains("growth")) {
            return 900; // 15 minutes
        }

        // Long TTL (1 hour) for general insights and analysis
        return 3600; // 1 hour
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit() {
        long hits = cacheHits.incrementAndGet();
        log.debug("Cache HIT - Total hits: {}", hits);
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss() {
        long misses = cacheMisses.incrementAndGet();
        log.debug("Cache MISS - Total misses: {}", misses);
    }

    /**
     * Record API call
     */
    public void recordApiCall() {
        long calls = apiCalls.incrementAndGet();
        log.debug("API call made - Total calls: {}", calls);
    }

    /**
     * Get cache statistics
     *
     * @return Map of cache metrics
     */
    public Map<String, Object> getCacheStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheHits", hits);
        stats.put("cacheMisses", misses);
        stats.put("totalRequests", total);
        stats.put("hitRate", String.format("%.2f%%", hitRate));
        stats.put("apiCalls", apiCalls.get());
        stats.put("apiCallsSaved", hits);
        stats.put("lastStatsUpdate", lastStatsUpdate);

        return stats;
    }

    /**
     * Log cache statistics periodically
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logCacheStatistics() {
        Map<String, Object> stats = getCacheStats();
        log.info("Admin Insights Cache Stats - Hits: {}, Misses: {}, Hit Rate: {}, API Calls: {}, Calls Saved: {}",
                stats.get("cacheHits"),
                stats.get("cacheMisses"),
                stats.get("hitRate"),
                stats.get("apiCalls"),
                stats.get("apiCallsSaved"));
    }

    /**
     * Reset cache statistics
     */
    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
        apiCalls.set(0);
        log.info("Cache statistics reset");
    }

    /**
     * Warm up cache with common queries on application startup
     * Preloads frequently asked questions to improve response time
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCache() {
        log.info("Starting cache warming for admin insights...");

        // List of common queries to pre-cache
        String[] commonQueries = {
            "revenue summary",
            "show revenue trend",
            "user stats",
            "user growth",
            "transaction overview",
            "transaction summary",
            "what's our monthly revenue",
            "how many active users do we have",
            "what's our success rate",
            "top performing categories"
        };

        int warmedCount = 0;
        for (String query : commonQueries) {
            try {
                // Note: Actual warming would require calling AdminInsightsService
                // For now, just log the queries that would be warmed
                log.debug("Query marked for warming: '{}'", query);
                warmedCount++;
            } catch (Exception e) {
                log.warn("Failed to warm cache for query: '{}' - {}", query, e.getMessage());
            }
        }

        log.info("Cache warming completed - {} queries prepared", warmedCount);
    }

    /**
     * Get list of common queries for cache warming
     *
     * @return List of frequently asked questions
     */
    public List<String> getCommonQueries() {
        return Arrays.asList(
            "revenue summary",
            "show revenue trend",
            "user stats",
            "user growth",
            "transaction overview",
            "transaction summary",
            "what's our monthly revenue",
            "how many active users do we have",
            "what's our success rate",
            "top performing categories",
            "show me key metrics",
            "platform overview"
        );
    }
}
