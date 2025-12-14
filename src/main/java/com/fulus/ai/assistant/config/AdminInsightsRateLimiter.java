package com.fulus.ai.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for admin insights API
 * 
 * Limits:
 * - 30 requests per minute per admin
 * - 100 requests per hour per admin
 * 
 * Uses simple token bucket algorithm with time windows
 */
@Component
@Slf4j
public class AdminInsightsRateLimiter {

    private static final int MINUTE_LIMIT = 30;
    private static final int HOUR_LIMIT = 100;
    private static final long MINUTE_WINDOW_MILLIS = 60_000; // 1 minute
    private static final long HOUR_WINDOW_MILLIS = 3_600_000; // 1 hour

    private final Map<UUID, RateLimitBucket> minuteBuckets = new ConcurrentHashMap<>();
    private final Map<UUID, RateLimitBucket> hourBuckets = new ConcurrentHashMap<>();

    /**
     * Check if admin can make a request
     * 
     * @param adminId Admin user ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(UUID adminId) {
        long now = Instant.now().toEpochMilli();

        RateLimitBucket minuteBucket = minuteBuckets.computeIfAbsent(adminId, 
                k -> new RateLimitBucket(MINUTE_LIMIT, MINUTE_WINDOW_MILLIS));
        RateLimitBucket hourBucket = hourBuckets.computeIfAbsent(adminId, 
                k -> new RateLimitBucket(HOUR_LIMIT, HOUR_WINDOW_MILLIS));

        boolean minuteAllowed = minuteBucket.tryConsume(now);
        if (!minuteAllowed) {
            log.warn("Rate limit exceeded for admin {} - minute limit ({} req/min)", adminId, MINUTE_LIMIT);
            return false;
        }

        boolean hourAllowed = hourBucket.tryConsume(now);
        if (!hourAllowed) {
            log.warn("Rate limit exceeded for admin {} - hour limit ({} req/hour)", adminId, HOUR_LIMIT);
            // Refund the minute bucket since hour limit was hit
            minuteBucket.refund();
            return false;
        }

        return true;
    }

    /**
     * Get remaining requests for admin in the current minute
     */
    public long getRemainingMinuteRequests(UUID adminId) {
        RateLimitBucket bucket = minuteBuckets.get(adminId);
        if (bucket == null) {
            return MINUTE_LIMIT;
        }
        bucket.reset(Instant.now().toEpochMilli());
        return bucket.getRemaining();
    }

    /**
     * Get remaining requests for admin in the current hour
     */
    public long getRemainingHourRequests(UUID adminId) {
        RateLimitBucket bucket = hourBuckets.get(adminId);
        if (bucket == null) {
            return HOUR_LIMIT;
        }
        bucket.reset(Instant.now().toEpochMilli());
        return bucket.getRemaining();
    }

    /**
     * Clear rate limit data for admin (for testing or admin override)
     */
    public void clearLimits(UUID adminId) {
        minuteBuckets.remove(adminId);
        hourBuckets.remove(adminId);
        log.info("Cleared rate limits for admin: {}", adminId);
    }

    /**
     * Simple token bucket for rate limiting
     */
    private static class RateLimitBucket {
        private final int capacity;
        private final long windowMillis;
        private final AtomicInteger tokens;
        private volatile long windowStart;

        public RateLimitBucket(int capacity, long windowMillis) {
            this.capacity = capacity;
            this.windowMillis = windowMillis;
            this.tokens = new AtomicInteger(capacity);
            this.windowStart = Instant.now().toEpochMilli();
        }

        public synchronized boolean tryConsume(long now) {
            reset(now);
            int current = tokens.get();
            if (current > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        public synchronized void refund() {
            if (tokens.get() < capacity) {
                tokens.incrementAndGet();
            }
        }

        public synchronized void reset(long now) {
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                tokens.set(capacity);
            }
        }

        public int getRemaining() {
            return tokens.get();
        }
    }
}

