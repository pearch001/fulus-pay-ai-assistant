package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

    List<ChatMessage> findByConversationIdOrderBySequenceNumberDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId ORDER BY m.sequenceNumber DESC")
    List<ChatMessage> findRecentMessages(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM ChatMessage m WHERE m.conversationId = :conversationId")
    Integer findMaxSequenceNumber(@Param("conversationId") UUID conversationId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId")
    Long countByConversationId(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.timestamp < :cutoffDate")
    int deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query(value = "DELETE FROM chat_messages WHERE id IN (SELECT id FROM chat_messages WHERE conversation_id = :conversationId ORDER BY sequence_number ASC LIMIT :limit)", nativeQuery = true)
    void deleteOldestMessages(@Param("conversationId") UUID conversationId, @Param("limit") int limit);
}
