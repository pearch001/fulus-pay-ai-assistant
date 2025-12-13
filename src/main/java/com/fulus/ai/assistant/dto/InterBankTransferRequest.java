package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterBankTransferRequest {

    @NotBlank(message = "Recipient account number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Account number must be 10 digits")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.0", message = "Minimum transfer amount is ₦100")
    @DecimalMax(value = "5000000.0", message = "Maximum transfer amount is ₦5,000,000")
    private Double amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
