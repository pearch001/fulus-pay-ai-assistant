package com.fulus.ai.assistant.service.chat;

import com.fulus.ai.assistant.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring-managed service for conversation operations
 * This service is needed because PostgreSQLChatMemory is not a Spring bean
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * Update conversation metadata (message count and tokens)
     * This method ensures the @Modifying query runs in a proper transactional context
     */
    @Transactional
    public void incrementMessageCountAndTokens(UUID conversationId, int tokens, LocalDateTime timestamp) {
        log.debug("Updating conversation {} with {} tokens", conversationId, tokens);
        conversationRepository.incrementMessageCountAndTokens(conversationId, tokens, timestamp);
    }
}
