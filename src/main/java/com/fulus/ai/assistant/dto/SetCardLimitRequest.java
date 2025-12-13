package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetCardLimitRequest {

    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "1000.0", message = "Daily limit must be at least ₦1,000")
    @DecimalMax(value = "5000000.0", message = "Daily limit cannot exceed ₦5,000,000")
    private Double dailyLimit;

    @NotNull(message = "Transaction limit is required")
    @DecimalMin(value = "100.0", message = "Transaction limit must be at least ₦100")
    @DecimalMax(value = "1000000.0", message = "Transaction limit cannot exceed ₦1,000,000")
    private Double transactionLimit;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
