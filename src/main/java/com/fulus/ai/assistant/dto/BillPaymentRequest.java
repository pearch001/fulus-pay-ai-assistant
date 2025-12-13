package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.BillType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPaymentRequest {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Bill type is required")
    private BillType billType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @Max(value = 1000000, message = "Amount must not exceed 1,000,000")
    private Double amount;

    @NotBlank(message = "Account number is required")
    @Size(min = 4, max = 20, message = "Account number must be between 4 and 20 characters")
    private String accountNumber;
}
