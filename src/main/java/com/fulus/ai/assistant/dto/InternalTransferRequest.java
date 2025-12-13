package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferRequest {

    @NotBlank(message = "Recipient phone number or account number is required")
    private String recipientIdentifier; // Can be phone number or account number

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₦1")
    @DecimalMax(value = "10000000.0", message = "Amount cannot exceed ₦10,000,000")
    private Double amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
