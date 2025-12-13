package com.fulus.ai.assistant.exception;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(double amount) {
        super(String.format("Invalid amount: %.2f. Amount must be greater than 0", amount));
    }
}
