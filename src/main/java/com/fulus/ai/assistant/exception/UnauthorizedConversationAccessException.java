package com.fulus.ai.assistant.exception;

import java.util.UUID;

/**
 * Exception thrown when an admin attempts to access a conversation they don't own
 */
public class UnauthorizedConversationAccessException extends RuntimeException {

    private final UUID adminId;
    private final UUID conversationId;

    public UnauthorizedConversationAccessException(UUID adminId, UUID conversationId) {
        super(String.format("Admin %s is not authorized to access conversation %s", adminId, conversationId));
        this.adminId = adminId;
        this.conversationId = conversationId;
    }

    public UnauthorizedConversationAccessException(String message) {
        super(message);
        this.adminId = null;
        this.conversationId = null;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public UUID getConversationId() {
        return conversationId;
    }
}

