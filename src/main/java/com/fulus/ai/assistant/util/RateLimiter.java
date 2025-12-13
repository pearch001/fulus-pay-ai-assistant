package com.fulus.ai.assistant.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sliding window rate limiter
 * Thread-safe implementation using ConcurrentHashMap
 */
@Slf4j
@Component
public class RateLimiter {

    // Map of key -> queue of request timestamps
    private final Map<String, Queue<Long>> requestMap = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed based on rate limit
     *
     * @param key          Unique identifier for rate limiting (e.g., "batch-sync:userId")
     * @param maxRequests  Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String key, int maxRequests, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        // Get or create queue for this key
        Queue<Long> requestTimestamps = requestMap.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Remove expired timestamps (outside window)
        synchronized (requestTimestamps) {
            requestTimestamps.removeIf(timestamp -> timestamp < windowStart);

            // Check if within limit
            if (requestTimestamps.size() < maxRequests) {
                requestTimestamps.offer(now);
                log.debug("Request allowed for key: {}, current count: {}/{}", key, requestTimestamps.size(), maxRequests);
                return true;
            } else {
                log.warn("Rate limit exceeded for key: {}, limit: {} requests per {} seconds",
                        key, maxRequests, windowSeconds);
                return false;
            }
        }
    }

    /**
     * Get current request count for a key
     *
     * @param key           Unique identifier
     * @param windowSeconds Time window in seconds
     * @return Number of requests in current window
     */
    public int getCurrentRequestCount(String key, int windowSeconds) {
        Queue<Long> requestTimestamps = requestMap.get(key);
        if (requestTimestamps == null) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        synchronized (requestTimestamps) {
            requestTimestamps.removeIf(timestamp -> timestamp < windowStart);
            return requestTimestamps.size();
        }
    }

    /**
     * Reset rate limit for a key
     *
     * @param key Unique identifier
     */
    public void reset(String key) {
        requestMap.remove(key);
        log.debug("Rate limit reset for key: {}", key);
    }

    /**
     * Clear all rate limit data (for testing or cleanup)
     */
    public void clearAll() {
        requestMap.clear();
        log.info("All rate limit data cleared");
    }

    /**
     * Get time until next request is allowed
     *
     * @param key           Unique identifier
     * @param maxRequests   Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return Milliseconds until next request is allowed, or 0 if request can be made now
     */
    public long getTimeUntilNextRequest(String key, int maxRequests, int windowSeconds) {
        Queue<Long> requestTimestamps = requestMap.get(key);
        if (requestTimestamps == null || requestTimestamps.isEmpty()) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        synchronized (requestTimestamps) {
            requestTimestamps.removeIf(timestamp -> timestamp < windowStart);

            if (requestTimestamps.size() < maxRequests) {
                return 0;
            }

            // Find oldest timestamp
            Long oldestTimestamp = requestTimestamps.peek();
            if (oldestTimestamp == null) {
                return 0;
            }

            long timeUntilExpiry = (oldestTimestamp + (windowSeconds * 1000L)) - now;
            return Math.max(0, timeUntilExpiry);
        }
    }
}
