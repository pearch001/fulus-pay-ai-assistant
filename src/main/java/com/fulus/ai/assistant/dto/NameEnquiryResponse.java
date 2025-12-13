package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameEnquiryResponse {

    private boolean success;
    private String message;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String bankName;

    public static NameEnquiryResponse success(String accountNumber, String accountName, String bankCode, String bankName) {
        return NameEnquiryResponse.builder()
                .success(true)
                .message("Account name retrieved successfully")
                .accountNumber(accountNumber)
                .accountName(accountName)
                .bankCode(bankCode)
                .bankName(bankName)
                .build();
    }

    public static NameEnquiryResponse failure(String message) {
        return NameEnquiryResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
