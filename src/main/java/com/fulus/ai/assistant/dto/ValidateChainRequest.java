package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for chain validation (without syncing)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateChainRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Transactions list cannot be empty")
    @Size(max = 100, message = "Maximum 100 transactions allowed per validation")
    @Valid
    private List<OfflineTransactionDTO> transactions;
}
