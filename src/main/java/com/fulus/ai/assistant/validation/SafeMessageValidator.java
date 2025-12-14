package com.fulus.ai.assistant.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator implementation for SafeMessage annotation
 * 
 * Checks for:
 * - SQL injection patterns (SELECT, INSERT, UPDATE, DELETE, DROP, etc.)
 * - XSS attempts (script tags, event handlers, javascript:)
 * - Excessive special characters
 * - Reasonable message structure
 */
@Slf4j
public class SafeMessageValidator implements ConstraintValidator<SafeMessage, String> {

    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i).*(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE)\\b).*"
    );

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i).*(<script|javascript:|onerror=|onload=|onclick=|<iframe|eval\\(|document\\.|window\\.).*"
    );

    // Excessive special characters (more than 30% of message)
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s,.!?;:'\"-]");

    // Command injection patterns
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
            ".*[;&|`$(){}\\[\\]<>].*"
    );

    @Override
    public void initialize(SafeMessage constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty validation
        }

        // 1. Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(value).matches()) {
            log.warn("Potential SQL injection attempt detected: {}", sanitizeForLogging(value));
            setCustomMessage(context, "Message contains potentially dangerous SQL keywords");
            return false;
        }

        // 2. Check for XSS patterns
        if (XSS_PATTERN.matcher(value).matches()) {
            log.warn("Potential XSS attempt detected: {}", sanitizeForLogging(value));
            setCustomMessage(context, "Message contains potentially dangerous scripts");
            return false;
        }

        // 3. Check for command injection patterns
        if (COMMAND_INJECTION_PATTERN.matcher(value).matches()) {
            log.warn("Potential command injection attempt detected: {}", sanitizeForLogging(value));
            setCustomMessage(context, "Message contains potentially dangerous command characters");
            return false;
        }

        // 4. Check for excessive special characters
        if (!isReasonableSpecialCharRatio(value)) {
            log.warn("Excessive special characters detected: {}", sanitizeForLogging(value));
            setCustomMessage(context, "Message contains too many special characters");
            return false;
        }

        // 5. Check for reasonable length (additional check)
        if (value.length() < 2 || value.length() > 2000) {
            setCustomMessage(context, "Message length must be between 2 and 2000 characters");
            return false;
        }

        return true;
    }

    /**
     * Check if the ratio of special characters is reasonable
     * Returns false if more than 30% of characters are special characters
     */
    private boolean isReasonableSpecialCharRatio(String value) {
        int totalChars = value.length();
        long specialCharsCount = SPECIAL_CHARS_PATTERN.matcher(value).results().count();
        
        double ratio = (double) specialCharsCount / totalChars;
        return ratio <= 0.3; // Allow up to 30% special characters
    }

    /**
     * Set custom error message in validation context
     */
    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    /**
     * Sanitize message for logging (truncate and remove sensitive patterns)
     */
    private String sanitizeForLogging(String value) {
        if (value == null) {
            return "null";
        }
        String sanitized = value.replaceAll("[<>\"';]", "*");
        return sanitized.length() > 50 ? sanitized.substring(0, 50) + "..." : sanitized;
    }
}

