package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameEnquiryRequest {

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Account number must be 10 digits")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;
}
