package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for paying bills
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayBillRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("The unique identifier (UUID) of the user paying the bill")
    private String userId;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Type of bill to pay. Valid values: ELECTRICITY, WATER, AIRTIME, DATA, CABLE_TV")
    private String billType;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Amount to pay in currency (e.g., 5000 for ₦5,000)")
    private Double amount;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Bill account number or meter number (e.g., '1234567890' for electricity meter)")
    private String accountNumber;
}
