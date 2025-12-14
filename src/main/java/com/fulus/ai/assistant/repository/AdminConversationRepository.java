package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.AdminConversation;
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
public interface AdminConversationRepository extends JpaRepository<AdminConversation, UUID> {

    /**
     * Find admin conversation by unique conversation ID
     */
    Optional<AdminConversation> findByConversationId(UUID conversationId);

    /**
     * Find all conversations for a specific admin, ordered by most recently updated
     */
    List<AdminConversation> findByAdminIdOrderByUpdatedAtDesc(UUID adminId);

    /**
     * Find active conversation for a specific admin
     */
    Optional<AdminConversation> findByAdminIdAndIsActiveTrue(UUID adminId);

    /**
     * Count total conversations for a specific admin
     */
    long countByAdminId(UUID adminId);

    /**
     * Find conversations with message count exceeding the threshold
     * Useful for identifying highly active conversations
     */
    @Query("SELECT ac FROM AdminConversation ac WHERE ac.messageCount > :threshold ORDER BY ac.messageCount DESC")
    List<AdminConversation> findConversationsAboveMessageThreshold(@Param("threshold") Integer threshold);

    /**
     * Find conversations created before a specific date (for cleanup)
     * @param cutoffDate - conversations created before this date
     * @return List of old conversations
     */
    @Query("SELECT ac FROM AdminConversation ac WHERE ac.createdAt < :cutoffDate ORDER BY ac.createdAt ASC")
    List<AdminConversation> findConversationsCreatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old inactive conversations created before a specific date
     * @param cutoffDate - delete conversations created before this date
     * @param isActive - filter by active status
     * @return number of deleted conversations
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AdminConversation ac WHERE ac.createdAt < :cutoffDate AND ac.isActive = :isActive")
    int deleteOldConversations(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("isActive") Boolean isActive);

    /**
     * Find all active conversations for an admin
     */
    List<AdminConversation> findByAdminIdAndIsActiveTrueOrderByUpdatedAtDesc(UUID adminId);

    /**
     * Update conversation activity status
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminConversation ac SET ac.isActive = :isActive, ac.updatedAt = :updatedAt WHERE ac.conversationId = :conversationId")
    int updateConversationStatus(
        @Param("conversationId") UUID conversationId,
        @Param("isActive") Boolean isActive,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Increment message count and total tokens
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminConversation ac SET ac.messageCount = ac.messageCount + 1, ac.totalTokens = ac.totalTokens + :tokens, ac.updatedAt = :updatedAt WHERE ac.conversationId = :conversationId")
    int incrementMessageCountAndTokens(
        @Param("conversationId") UUID conversationId,
        @Param("tokens") Long tokens,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}

