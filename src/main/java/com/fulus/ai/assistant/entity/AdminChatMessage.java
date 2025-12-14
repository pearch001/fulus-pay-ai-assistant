package com.fulus.ai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fulus.ai.assistant.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin_chat_messages", indexes = {
    @Index(name = "idx_admin_chat_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_admin_chat_created_at", columnList = "createdAt"),
    @Index(name = "idx_admin_chat_conversation_sequence", columnList = "conversation_id, sequenceNumber")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", referencedColumnName = "conversationId", insertable = false, updatable = false)
    private AdminConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column
    private Integer tokenCount;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private Long processingTimeMs;
}

