package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.BVNVerificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock BVN Verification Service
 *
 * Simulates BVN verification with Nigerian Bank Verification Number system
 *
 * Mock Rules:
 * - BVN ending in even number = APPROVED
 * - BVN ending in odd number = REJECTED
 * - Simulates 500ms API delay
 * - Returns mock user details (name, DOB, phone)
 */
@Service
@Slf4j
public class BVNVerificationService {

    private static final long MOCK_API_DELAY_MS = 500;

    // Mock Nigerian names for testing
    private static final String[] NIGERIAN_FIRST_NAMES = {
        "Chukwuemeka", "Oluwaseun", "Ngozi", "Abiodun", "Chiamaka",
        "Ibrahim", "Fatima", "Yusuf", "Amina", "Emeka",
        "Blessing", "Emmanuel", "Grace", "David", "Mary"
    };

    private static final String[] NIGERIAN_LAST_NAMES = {
        "Okonkwo", "Adeyemi", "Nwosu", "Okoro", "Eze",
        "Mohammed", "Bello", "Abdullahi", "Abubakar", "Okeke",
        "Williams", "Johnson", "Okafor", "Chibueze", "Adeleke"
    };

    /**
     * Verify BVN and return mock user details
     *
     * @param bvn Bank Verification Number (11 digits)
     * @param expectedName Expected full name for cross-checking
     * @param expectedDateOfBirth Expected date of birth for validation
     * @return BVNVerificationResponse
     */
    public BVNVerificationResponse verifyBVN(String bvn, String expectedName, LocalDate expectedDateOfBirth) {
        log.info("Verifying BVN: {} for user: {}", maskBVN(bvn), expectedName);

        // Validate BVN format
        if (bvn == null || !bvn.matches("^\\d{11}$")) {
            log.warn("Invalid BVN format: {}", bvn);
            return BVNVerificationResponse.failure("Invalid BVN format. Must be exactly 11 digits.");
        }

        // Simulate API call delay
        simulateAPIDelay();

        // Mock verification logic: BVN ending in even number = approved
        char lastDigit = bvn.charAt(bvn.length() - 1);
        int lastDigitValue = Character.getNumericValue(lastDigit);

        /*if (lastDigitValue % 2 != 0) {
            // Odd number = rejected (for testing)
            log.warn("BVN verification failed: BVN ending in odd number (mock rejection)");
            return BVNVerificationResponse.failure("BVN verification failed. Please ensure your BVN is correct.");
        }*/

        // Generate mock user details based on BVN
        String mockFullName = generateMockName(bvn);
        LocalDate mockDateOfBirth = generateMockDateOfBirth(bvn);
        String mockPhoneNumber = generateMockPhoneNumber(bvn);

        log.info("BVN verification successful. Mock name: {}, Mock DOB: {}",
                mockFullName, mockDateOfBirth);

        return BVNVerificationResponse.success(bvn, mockFullName, mockDateOfBirth, mockPhoneNumber);
    }

    /**
     * Check name similarity between provided name and BVN-registered name
     *
     * @param providedName Name provided by user during registration
     * @param bvnName Name returned from BVN verification
     * @return true if names are similar enough
     */
    public boolean checkNameSimilarity(String providedName, String bvnName) {
        if (providedName == null || bvnName == null) {
            return false;
        }

        // Normalize names: lowercase, remove extra spaces
        String normalizedProvided = normalizeString(providedName);
        String normalizedBVN = normalizeString(bvnName);

        log.debug("Checking name similarity: '{}' vs '{}'", normalizedProvided, normalizedBVN);

        // Exact match (case-insensitive)
        if (normalizedProvided.equals(normalizedBVN)) {
            log.info("Name match: Exact match");
            return true;
        }

        // Check if one contains the other
        if (normalizedProvided.contains(normalizedBVN) || normalizedBVN.contains(normalizedProvided)) {
            log.info("Name match: Partial match (containment)");
            return true;
        }

        // Split into words and check overlap
        String[] providedWords = normalizedProvided.split("\\s+");
        String[] bvnWords = normalizedBVN.split("\\s+");

        int matchCount = 0;
        for (String providedWord : providedWords) {
            for (String bvnWord : bvnWords) {
                if (providedWord.equals(bvnWord) && providedWord.length() > 2) {
                    matchCount++;
                    break;
                }
            }
        }

        // At least 50% of words must match (minimum 1 word)
        int minWords = Math.min(providedWords.length, bvnWords.length);
        boolean similar = matchCount >= Math.max(1, minWords / 2);

        if (similar) {
            log.info("Name match: {} of {} words matched", matchCount, minWords);
        } else {
            log.warn("Name mismatch: Only {} of {} words matched", matchCount, minWords);
        }

        return similar;
    }

    /**
     * Simulate API call delay
     */
    private void simulateAPIDelay() {
        try {
            Thread.sleep(MOCK_API_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("API delay simulation interrupted", e);
        }
    }

    /**
     * Generate mock name based on BVN
     */
    private String generateMockName(String bvn) {
        // Use BVN digits to consistently generate same name for same BVN
        int seed = Integer.parseInt(bvn.substring(0, 4));
        Random random = new Random(seed);

        String firstName = NIGERIAN_FIRST_NAMES[random.nextInt(NIGERIAN_FIRST_NAMES.length)];
        String lastName = NIGERIAN_LAST_NAMES[random.nextInt(NIGERIAN_LAST_NAMES.length)];

        return firstName + " " + lastName;
    }

    /**
     * Generate mock date of birth based on BVN
     */
    private LocalDate generateMockDateOfBirth(String bvn) {
        // Use BVN to generate consistent DOB (age between 18-60 years)
        int seed = Integer.parseInt(bvn.substring(4, 8));
        Random random = new Random(seed);

        int yearsAgo = 18 + random.nextInt(43); // 18-60 years old
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28); // Safe day range

        return LocalDate.now().minusYears(yearsAgo).withMonth(month).withDayOfMonth(day);
    }

    /**
     * Generate mock phone number based on BVN
     */
    private String generateMockPhoneNumber(String bvn) {
        // Use last 8 digits of BVN for phone number
        String prefix = "080"; // Nigerian MTN prefix
        String suffix = bvn.substring(3, 11);
        return prefix + suffix;
    }

    /**
     * Normalize string for comparison
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        return str.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Mask BVN for logging (show first 3 and last 2 digits)
     */
    private String maskBVN(String bvn) {
        if (bvn == null || bvn.length() < 5) {
            return "***";
        }
        return bvn.substring(0, 3) + "******" + bvn.substring(bvn.length() - 2);
    }

    /**
     * Check if BVN is approved (for testing purposes)
     */
    public boolean isApprovedBVN(String bvn) {
        if (bvn == null || bvn.length() != 11) {
            return false;
        }
        char lastDigit = bvn.charAt(10);
        int lastDigitValue = Character.getNumericValue(lastDigit);
        return lastDigitValue % 2 == 0;
    }
}
