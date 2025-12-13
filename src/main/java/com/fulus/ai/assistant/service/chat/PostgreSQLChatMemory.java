package com.fulus.ai.assistant.service.chat;

import com.fulus.ai.assistant.entity.ChatMessage;
import com.fulus.ai.assistant.entity.Conversation;
import com.fulus.ai.assistant.enums.MessageType;
import com.fulus.ai.assistant.repository.ChatMessageRepository;
import com.fulus.ai.assistant.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PostgreSQLChatMemory implements ChatMemory {

    private final UUID conversationId;
    private final UUID userId;
    private final int maxMessages;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    public PostgreSQLChatMemory(
            UUID userId,
            int maxMessages,
            ChatMessageRepository chatMessageRepository,
            ConversationRepository conversationRepository) {

        this.userId = userId;
        this.maxMessages = maxMessages;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;

        // Get or create conversation for this user
        this.conversationId = getOrCreateConversation();

        log.debug("Initialized PostgreSQLChatMemory for user {} with conversationId {}", userId, conversationId);
    }

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        log.debug("Adding {} messages to conversation {}", messages.size(), conversationId);

        for (Message message : messages) {
            addMessage(message);
        }

        // Prune old messages if we exceed the limit
        pruneOldMessages();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId, int lastN) {
        log.debug("Retrieving last {} messages from conversation {}", lastN, conversationId);

        List<ChatMessage> chatMessages = chatMessageRepository.findRecentMessages(
                this.conversationId,
                PageRequest.of(0, lastN)
        );

        // Reverse to get chronological order
        Collections.reverse(chatMessages);

        return chatMessages.stream()
                .map(this::toSpringAIMessage)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        log.info("Clearing all messages from conversation {}", conversationId);

        chatMessageRepository.deleteByConversationId(this.conversationId);

        // Reset conversation metadata
        conversationRepository.findById(this.conversationId).ifPresent(conversation -> {
            conversation.setMessageCount(0);
            conversation.setTotalTokensUsed(0);
            conversationRepository.save(conversation);
        });
    }

    /**
     * Add a single message to the conversation
     */
    @Transactional
    private void addMessage(Message message) {
        Integer nextSequence = chatMessageRepository.findMaxSequenceNumber(conversationId) + 1;

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(message.getContent());
        chatMessage.setSequenceNumber(nextSequence);
        chatMessage.setTimestamp(LocalDateTime.now());

        // Determine message type and extract metadata
        MessageType messageType = determineMessageType(message);
        chatMessage.setMessageType(messageType);

        // Extract metadata from Spring AI message
        Map<String, Object> metadata = new HashMap<>();
        if (message.getMetadata() != null) {
            metadata.putAll(message.getMetadata());
        }
        chatMessage.setMetadata(metadata);

        // Estimate tokens (rough approximation: 1 token ≈ 4 characters)
        int estimatedTokens = estimateTokens(message.getContent());
        chatMessage.setTokensUsed(estimatedTokens);

        chatMessageRepository.save(chatMessage);

        // Update conversation metadata
        updateConversationMetadata(estimatedTokens);

        log.debug("Added message to conversation {}: type={}, sequence={}, tokens={}",
                conversationId, messageType, nextSequence, estimatedTokens);
    }

    /**
     * Prune old messages when exceeding max limit
     */
    @Transactional
    private void pruneOldMessages() {
        Long messageCount = chatMessageRepository.countByConversationId(conversationId);

        if (messageCount > maxMessages) {
            int toDelete = (int) (messageCount - maxMessages);
            log.info("Pruning {} old messages from conversation {}", toDelete, conversationId);
            chatMessageRepository.deleteOldestMessages(conversationId, toDelete);
        }
    }

    /**
     * Get or create conversation for the user
     */
    @Transactional
    private UUID getOrCreateConversation() {
        Optional<Conversation> existing = conversationRepository.findByUserId(userId);

        if (existing.isPresent() && !existing.get().getArchived()) {
            return existing.get().getId();
        }

        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle("Chat - " + LocalDateTime.now().toString());
        conversation.setMessageCount(0);
        conversation.setTotalTokensUsed(0);
        conversation.setArchived(false);

        conversation = conversationRepository.save(conversation);
        log.info("Created new conversation {} for user {}", conversation.getId(), userId);

        return conversation.getId();
    }

    /**
     * Update conversation metadata
     */
    @Transactional
    private void updateConversationMetadata(int tokens) {
        conversationRepository.incrementMessageCountAndTokens(
                conversationId,
                tokens,
                LocalDateTime.now()
        );
    }

    /**
     * Convert ChatMessage to Spring AI Message
     */
    private Message toSpringAIMessage(ChatMessage chatMessage) {
        String content = chatMessage.getContent();

        return switch (chatMessage.getMessageType()) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL_EXECUTION -> new AssistantMessage(content); // Map to assistant for now
        };
    }

    /**
     * Determine message type from Spring AI Message
     */
    private MessageType determineMessageType(Message message) {
        if (message instanceof UserMessage) {
            return MessageType.USER;
        } else if (message instanceof AssistantMessage) {
            return MessageType.ASSISTANT;
        } else if (message instanceof SystemMessage) {
            return MessageType.SYSTEM;
        } else {
            return MessageType.TOOL_EXECUTION;
        }
    }

    /**
     * Estimate token count (rough approximation)
     * OpenAI uses ~4 characters per token on average
     */
    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }
}
