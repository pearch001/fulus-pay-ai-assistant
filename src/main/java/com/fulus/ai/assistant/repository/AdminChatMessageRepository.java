package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.AdminChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminChatMessageRepository extends JpaRepository<AdminChatMessage, UUID> {

    /**
     * Find all messages for a conversation ordered by sequence number (ascending)
     * Returns messages in chronological order
     */
    List<AdminChatMessage> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

    /**
     * Find the most recent 20 messages for a conversation
     * Ordered by sequence number descending (most recent first)
     */
    List<AdminChatMessage> findTop20ByConversationIdOrderBySequenceNumberDesc(UUID conversationId);

    /**
     * Count total messages in a conversation
     */
    long countByConversationId(UUID conversationId);

    /**
     * Find the latest message in a conversation
     * Useful for getting the most recent message
     */
    Optional<AdminChatMessage> findTopByConversationIdOrderBySequenceNumberDesc(UUID conversationId);

    /**
     * Find the first message in a conversation
     * Useful for getting conversation context
     */
    Optional<AdminChatMessage> findTopByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

    /**
     * Get the maximum sequence number for a conversation
     * Useful for determining the next sequence number
     */
    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM AdminChatMessage m WHERE m.conversationId = :conversationId")
    Integer findMaxSequenceNumber(@Param("conversationId") UUID conversationId);

    /**
     * Delete messages older than 30 days
     * Cleanup operation to maintain database size
     * @param cutoffDate - messages created before this date will be deleted
     * @return number of deleted messages
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AdminChatMessage m WHERE m.createdAt < :cutoffDate")
    int deleteMessagesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete all messages for a specific conversation
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AdminChatMessage m WHERE m.conversationId = :conversationId")
    int deleteByConversationId(@Param("conversationId") UUID conversationId);

    /**
     * Find messages by role (USER, ASSISTANT, SYSTEM)
     * Useful for analytics and filtering
     */
    @Query("SELECT m FROM AdminChatMessage m WHERE m.conversationId = :conversationId AND m.role = :role ORDER BY m.sequenceNumber ASC")
    List<AdminChatMessage> findByConversationIdAndRole(
        @Param("conversationId") UUID conversationId,
        @Param("role") String role
    );

    /**
     * Get total token usage for a conversation
     */
    @Query("SELECT COALESCE(SUM(m.tokenCount), 0) FROM AdminChatMessage m WHERE m.conversationId = :conversationId")
    Long sumTokenCountByConversationId(@Param("conversationId") UUID conversationId);

    /**
     * Get average processing time for AI responses in a conversation
     */
    @Query("SELECT COALESCE(AVG(m.processingTimeMs), 0) FROM AdminChatMessage m WHERE m.conversationId = :conversationId AND m.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime(@Param("conversationId") UUID conversationId);

    /**
     * Find messages with processing time exceeding threshold
     * Useful for performance monitoring
     */
    @Query("SELECT m FROM AdminChatMessage m WHERE m.processingTimeMs > :thresholdMs ORDER BY m.processingTimeMs DESC")
    List<AdminChatMessage> findSlowMessages(@Param("thresholdMs") Long thresholdMs);

    /**
     * Delete oldest messages in a conversation (keep most recent N messages)
     * Native query for better performance
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM admin_chat_messages WHERE id IN (SELECT id FROM admin_chat_messages WHERE conversation_id = :conversationId ORDER BY sequence_number ASC LIMIT :limit)", nativeQuery = true)
    int deleteOldestMessages(@Param("conversationId") UUID conversationId, @Param("limit") int limit);
}

