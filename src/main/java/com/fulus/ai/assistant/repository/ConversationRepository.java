package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.Conversation;
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
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserId(UUID userId);

    List<Conversation> findByUserIdAndArchivedFalse(UUID userId);

    @Query("SELECT c FROM Conversation c WHERE c.lastMessageAt < :cutoffDate AND c.archived = false")
    List<Conversation> findStaleConversations(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Conversation c SET c.messageCount = c.messageCount + 1, c.totalTokensUsed = c.totalTokensUsed + :tokens, c.lastMessageAt = :timestamp WHERE c.id = :conversationId")
    int incrementMessageCountAndTokens(
        @Param("conversationId") UUID conversationId,
        @Param("tokens") Integer tokens,
        @Param("timestamp") LocalDateTime timestamp
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Conversation c SET c.archived = true WHERE c.id = :conversationId")
    int archiveConversation(@Param("conversationId") UUID conversationId);
}
