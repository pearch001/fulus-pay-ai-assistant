package com.fulus.ai.assistant.util;

import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating virtual account numbers
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final UserRepository userRepository;

    /**
     * Generate unique 10-digit virtual account number
     * Format: 30XXXXXXXX (starts with 30)
     */
    public String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate 8 random digits
            long randomPart = (long) (Math.random() * 100000000);
            // Prefix with 30 (virtual account prefix)
            accountNumber = "30" + String.format("%08d", randomPart);
        } while (userRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }
}
