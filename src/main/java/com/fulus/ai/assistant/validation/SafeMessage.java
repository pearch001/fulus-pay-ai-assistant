package com.fulus.ai.assistant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure messages are safe from injection attacks
 *
 * Validates against:
 * - SQL injection patterns
 * - XSS attempts
 * - Excessive special characters
 * - Script tags and dangerous patterns
 */
@Documented
@Constraint(validatedBy = SafeMessageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeMessage {

    String message() default "Message contains potentially unsafe content";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

