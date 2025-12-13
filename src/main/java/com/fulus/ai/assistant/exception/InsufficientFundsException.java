package com.fulus.ai.assistant.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String userId, double requestedAmount, double availableBalance) {
        super(String.format("Insufficient funds for user %s. Requested: %.2f, Available: %.2f",
                userId, requestedAmount, availableBalance));
    }
}
