package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chain status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainStatusResponse {

    private String userId;
    private boolean chainExists;
    private boolean chainValid;
    private String lastSyncedHash;
    private LocalDateTime lastSyncedAt;
    private String currentHeadHash;
    private Integer totalTransactions;
    private Integer syncedCount;
    private Integer pendingCount;
    private Integer failedCount;
    private Integer conflictCount;
    private String genesisHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method for non-existent chain
     */
    public static ChainStatusResponse notFound(String userId) {
        return ChainStatusResponse.builder()
                .userId(userId)
                .chainExists(false)
                .chainValid(false)
                .totalTransactions(0)
                .syncedCount(0)
                .pendingCount(0)
                .failedCount(0)
                .conflictCount(0)
                .build();
    }
}
