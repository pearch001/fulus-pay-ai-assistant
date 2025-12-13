package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.CardStatus;
import com.fulus.ai.assistant.enums.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {

    private UUID id;
    private CardType cardType;
    private String maskedCardNumber;
    private String cardHolderName;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal dailyLimit;
    private BigDecimal transactionLimit;
    private Boolean smsAlertEnabled;
    private Boolean internationalTransactionsEnabled;
    private Boolean onlineTransactionsEnabled;
    private Boolean contactlessEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
}
