package com.fulus.ai.assistant.service.chat;

import com.fulus.ai.assistant.repository.ChatMessageRepository;
import com.fulus.ai.assistant.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for maintaining chat memory and pruning old messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMemoryMaintenanceService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    @Value("${spring.ai.chat.memory.prune-after-days:30}")
    private int pruneAfterDays;

    /**
     * Prune old messages from database
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void pruneOldMessages() {
        log.info("Starting scheduled pruning of old chat messages");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(pruneAfterDays);

            int deletedCount = chatMessageRepository.deleteOldMessages(cutoffDate);

            log.info("Pruned {} messages older than {} days", deletedCount, pruneAfterDays);

            // Also archive old conversations
            archiveStaleConversations();

        } catch (Exception e) {
            log.error("Error during message pruning", e);
        }
    }

    /**
     * Archive conversations that haven't had activity in 30 days
     */
    @Transactional
    public void archiveStaleConversations() {
        log.info("Archiving stale conversations");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            var staleConversations = conversationRepository.findStaleConversations(cutoffDate);

            for (var conversation : staleConversations) {
                conversationRepository.archiveConversation(conversation.getId());
                log.debug("Archived conversation: {}", conversation.getId());
            }

            log.info("Archived {} stale conversations", staleConversations.size());

        } catch (Exception e) {
            log.error("Error during conversation archiving", e);
        }
    }

    /**
     * Get database statistics
     */
    public java.util.Map<String, Object> getStats() {
        long totalMessages = chatMessageRepository.count();
        long totalConversations = conversationRepository.count();

        return java.util.Map.of(
                "totalMessages", totalMessages,
                "totalConversations", totalConversations,
                "pruneAfterDays", pruneAfterDays
        );
    }
}
