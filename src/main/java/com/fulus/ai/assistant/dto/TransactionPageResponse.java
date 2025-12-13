package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated transaction responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPageResponse {

    private List<TransactionDTO> transactions;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
