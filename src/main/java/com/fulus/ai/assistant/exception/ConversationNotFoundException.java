package com.fulus.ai.assistant.exception;

import java.util.UUID;

/**
 * Exception thrown when a conversation is not found
 */
public class ConversationNotFoundException extends RuntimeException {

    private final UUID conversationId;

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found: " + conversationId);
        this.conversationId = conversationId;
    }

    public ConversationNotFoundException(String message) {
        super(message);
        this.conversationId = null;
    }

    public UUID getConversationId() {
        return conversationId;
    }
}

