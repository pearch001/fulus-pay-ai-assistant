package com.fulus.ai.assistant.exception;

import java.util.UUID;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends RuntimeException {

    private final UUID adminId;
    private final long remainingMinute;
    private final long remainingHour;

    public RateLimitExceededException(UUID adminId, long remainingMinute, long remainingHour) {
        super(String.format("Rate limit exceeded for admin %s. Remaining: %d/min, %d/hour", 
                adminId, remainingMinute, remainingHour));
        this.adminId = adminId;
        this.remainingMinute = remainingMinute;
        this.remainingHour = remainingHour;
    }

    public RateLimitExceededException(String message) {
        super(message);
        this.adminId = null;
        this.remainingMinute = 0;
        this.remainingHour = 0;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public long getRemainingMinute() {
        return remainingMinute;
    }

    public long getRemainingHour() {
        return remainingHour;
    }
}

