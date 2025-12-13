package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankListResponse {

    private boolean success;
    private String message;
    private List<BankDTO> banks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDTO {
        private String bankCode;
        private String bankName;
    }

    public static BankListResponse success(List<BankDTO> banks) {
        return BankListResponse.builder()
                .success(true)
                .message("Banks retrieved successfully")
                .banks(banks)
                .build();
    }
}
