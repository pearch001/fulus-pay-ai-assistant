package com.fulus.ai.assistant.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for PIN validation
 */
public class PinValidator {

    private static final Set<String> WEAK_PINS = Set.of(
            // Sequential ascending
            "0123", "1234", "2345", "3456", "4567", "5678", "6789",
            "01234", "12345", "23456", "34567", "45678", "56789",
            "012345", "123456", "234567", "345678", "456789",

            // Sequential descending
            "9876", "8765", "7654", "6543", "5432", "4321", "3210",
            "98765", "87654", "76543", "65432", "54321", "43210",
            "987654", "876543", "765432", "654321", "543210",

            // Repeating digits
            "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
            "00000", "11111", "22222", "33333", "44444", "55555", "66666", "77777", "88888", "99999",
            "000000", "111111", "222222", "333333", "444444", "555555", "666666", "777777", "888888", "999999",

            // Common patterns
            "1212", "2121", "1313", "3131",
            "12121", "21212", "13131", "31313",
            "121212", "212121", "131313", "313131"
    );

    /**
     * Validate PIN strength
     *
     * @param pin PIN to validate
     * @return true if PIN is strong, false otherwise
     * @throws IllegalArgumentException if PIN is weak with description
     */
    public static void validatePinStrength(String pin) {
        // Check format
        if (pin == null || !pin.matches("^\\d{4,6}$")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }

        // Check if in weak PIN list
        if (WEAK_PINS.contains(pin)) {
            throw new IllegalArgumentException("PIN is too weak. Avoid sequential or repeating digits.");
        }

        // Check for all same digits (fallback in case not in set)
        if (isAllSameDigits(pin)) {
            throw new IllegalArgumentException("PIN cannot contain all same digits.");
        }

        // Check for sequential digits
        if (isSequential(pin)) {
            throw new IllegalArgumentException("PIN cannot be sequential (e.g., 1234, 4321).");
        }
    }

    /**
     * Check if all digits in PIN are the same
     */
    private static boolean isAllSameDigits(String pin) {
        char firstDigit = pin.charAt(0);
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) != firstDigit) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if PIN is sequential (ascending or descending)
     */
    private static boolean isSequential(String pin) {
        boolean ascending = true;
        boolean descending = true;

        for (int i = 1; i < pin.length(); i++) {
            int current = Character.getNumericValue(pin.charAt(i));
            int previous = Character.getNumericValue(pin.charAt(i - 1));

            if (current != previous + 1) {
                ascending = false;
            }
            if (current != previous - 1) {
                descending = false;
            }
        }

        return ascending || descending;
    }

    /**
     * Generate random 6-digit OTP
     */
    public static String generateOTP() {
        int otp = (int) (Math.random() * 900000) + 100000; // 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Generate random reset token
     */
    public static String generateResetToken() {
        return java.util.UUID.randomUUID().toString();
    }
}
