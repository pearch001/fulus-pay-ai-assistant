package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_conversation_last_message", columnList = "lastMessageAt"),
    @Index(name = "idx_conversation_archived", columnList = "archived")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(length = 255)
    private String title;

    @Column(nullable = false)
    private Integer messageCount = 0;

    @Column(nullable = false)
    private Integer totalTokensUsed = 0;

    @Column
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean archived = false;
}
