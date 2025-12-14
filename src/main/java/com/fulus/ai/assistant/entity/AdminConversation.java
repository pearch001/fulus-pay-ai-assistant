package com.fulus.ai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin Conversation Entity
 *
 * Represents a business insights chat conversation between an admin and the AI assistant.
 * Each conversation tracks:
 * - Admin who initiated it
 * - Unique conversation identifier
 * - Conversation subject/topic
 * - Message count and token usage
 * - Active status
 * - Associated chat messages
 */
@Entity
@Table(name = "admin_conversations", indexes = {
    @Index(name = "idx_admin_conversation_admin_id", columnList = "adminId"),
    @Index(name = "idx_admin_conversation_id", columnList = "conversationId", unique = true),
    @Index(name = "idx_admin_conversation_active", columnList = "isActive"),
    @Index(name = "idx_admin_conversation_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Admin user who owns this conversation
     * References User entity (must have ADMIN role)
     */
    @Column(nullable = false)
    private UUID adminId;

    /**
     * Unique identifier for this conversation
     * Used in API requests to continue existing conversations
     */
    @Column(nullable = false, unique = true)
    private UUID conversationId;

    /**
     * Optional subject/topic of the conversation
     * Auto-generated based on conversation content
     */
    @Column(length = 500)
    private String subject;

    /**
     * Summary of the conversation (auto-generated after 10+ messages)
     * Helps maintain context in long conversations
     */
    @Column(columnDefinition = "TEXT")
    private String conversationSummary;

    /**
     * Whether this conversation is active
     * Soft-delete mechanism
     */
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Total number of messages in this conversation
     * Incremented with each user and assistant message
     */
    @Column(nullable = false)
    private Integer messageCount = 0;

    /**
     * Total tokens consumed across all messages
     * Used for cost tracking and analytics
     */
    @Column(nullable = false)
    private Long totalTokens = 0L;

    /**
     * When the conversation was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     * Updated with each new message
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * All messages in this conversation
     * Cascade delete ensures cleanup when conversation is deleted
     */
    @JsonIgnore
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AdminChatMessage> messages = new ArrayList<>();

    /**
     * Helper method to add a message to this conversation
     */
    public void addMessage(AdminChatMessage message) {
        messages.add(message);
        message.setConversation(this);
        this.messageCount++;
        if (message.getTokenCount() != null) {
            this.totalTokens += message.getTokenCount();
        }
    }

    /**
     * Helper method to check if conversation should be summarized
     */
    public boolean shouldGenerateSummary() {
        return messageCount > 0 && messageCount % 10 == 0 && conversationSummary == null;
    }
}

