package com.fulus.ai.assistant.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final DateTimeFormatter HUMAN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.error("Insufficient funds error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INSUFFICIENT_FUNDS");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.error("User not found error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_FOUND");
    }

    @ExceptionHandler(SavingsAccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSavingsAccountNotFound(SavingsAccountNotFoundException ex) {
        log.error("Savings account not found error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "SAVINGS_ACCOUNT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAmount(InvalidAmountException ex) {
        log.error("Invalid amount error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_AMOUNT");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT");
    }

    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAIServiceException(OpenAIServiceException ex) {
        log.error("OpenAI service error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "AI_SERVICE_ERROR");
    }

    @ExceptionHandler(ChatMemoryException.class)
    public ResponseEntity<Map<String, Object>> handleChatMemoryException(ChatMemoryException ex) {
        log.error("Chat memory error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "CHAT_MEMORY_ERROR");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String humanTs = LocalDateTime.now().format(HUMAN_FORMAT);
        String isoTs = ZonedDateTime.now(ZoneOffset.UTC).toString();

        // Build a detailed message similar to the example provided
        String message = "Data Validation Failed";

        // Extract field-level errors from the validation exception
        var fieldErrors = new java.util.ArrayList<Map<String, Object>>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            Map<String, Object> fieldError = new LinkedHashMap<>();
            fieldError.put("field", error.getField());
            fieldError.put("rejectedValue", error.getRejectedValue());
            fieldError.put("message", error.getDefaultMessage());
            fieldErrors.add(fieldError);
        });

        // Log the exception with stacktrace
        log.error("Validation exception: {}", message, ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", 400);
        body.put("message", message);
        body.put("success", false);
        body.put("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        String humanTs = LocalDateTime.now().format(HUMAN_FORMAT);
        String isoTs = ZonedDateTime.now(ZoneOffset.UTC).toString();

        String message = ex.getMessage();

        log.error("Unexpected exception: {}", message, ex);

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("success", false);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String errorCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        body.put("message", status.getReasonPhrase());
        body.put("errorCode", errorCode);

        return new ResponseEntity<>(body, status);
    }
}
