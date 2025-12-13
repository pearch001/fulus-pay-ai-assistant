package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.KycStatus;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for user profile management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    /**
     * Get user profile by user ID
     */
    public UserProfileResponse getUserProfile(UUID userId) {
        log.info("Fetching profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserProfileResponse.fromUser(user);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update name if provided
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
            log.info("Updated name for user {}: {}", userId, request.getName());
        }

        // Update email if provided (with duplicate check)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // Check if email is different from current
            if (!request.getEmail().equals(user.getEmail())) {
                // Check if email already exists
                if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                    log.warn("Email update failed for user {}: Email already exists", userId);
                    throw new IllegalArgumentException("Email already registered");
                }

                user.setEmail(request.getEmail());
                log.info("Updated email for user {}: {}", userId, request.getEmail());
            }
        }

        user = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", userId);

        return UserProfileResponse.fromUser(user);
    }

    /**
     * Get wallet details
     */
    public WalletResponse getWallet(UUID userId) {
        log.info("Fetching wallet details for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Count pending transactions
        Integer pendingCount = transactionRepository.countByUserIdAndStatus(userId, TransactionStatus.PENDING);

        log.info("Wallet retrieved for user {}: Balance=₦{}, Pending={}",
                userId, user.getBalance(), pendingCount);

        return WalletResponse.active(user.getBalance(), user.getAccountNumber(), pendingCount);
    }

    /**
     * Verify identity with document upload (KYC)
     */
    @Transactional
    public KycVerificationResponse verifyIdentity(
            UUID userId,
            KycVerificationRequest request,
            MultipartFile documentFile) {

        log.info("KYC verification request for user {}: documentType={}", userId, request.getDocumentType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if already verified
        if (user.getKycStatus() == KycStatus.ENHANCED) {
            log.warn("KYC verification attempt for already verified user: {}", userId);
            return KycVerificationResponse.builder()
                    .success(true)
                    .kycStatus(KycStatus.ENHANCED)
                    .message("Account is already enhanced verified.")
                    .documentType(user.getKycDocumentType())
                    .verifiedAt(user.getKycVerifiedAt())
                    .build();
        }

        // Validate file if provided
        String documentUrl = null;
        if (documentFile != null && !documentFile.isEmpty()) {
            try {
                documentUrl = saveKycDocument(userId, documentFile, request.getDocumentType());
                log.info("KYC document saved for user {}: {}", userId, documentUrl);
            } catch (IOException e) {
                log.error("Failed to save KYC document for user {}", userId, e);
                throw new RuntimeException("Failed to save document. Please try again.");
            }
        }

        // Mock verification logic (for PoC)
        boolean verificationSuccess = performMockVerification(request, user);

        if (verificationSuccess) {
            // Update user KYC status
            user.setKycStatus(KycStatus.ENHANCED);
            user.setKycDocumentType(request.getDocumentType());
            user.setKycDocumentUrl(documentUrl);
            user.setKycVerifiedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("KYC verification successful for user {}: Enhanced status granted", userId);

            return KycVerificationResponse.success(
                    KycStatus.ENHANCED,
                    request.getDocumentType(),
                    user.getKycVerifiedAt()
            );
        } else {
            // Mark as rejected
            user.setKycStatus(KycStatus.REJECTED);
            userRepository.save(user);

            log.warn("KYC verification failed for user {}: Document validation failed", userId);

            return KycVerificationResponse.failure("Document validation failed. Please ensure document is clear and valid.");
        }
    }

    /**
     * Save KYC document to file system
     */
    private String saveKycDocument(UUID userId, MultipartFile file, String documentType) throws IOException {
        // Create upload directory if it doesn't exist
        String uploadDir = System.getProperty("java.io.tmpdir") + "/fulus-kyc";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = userId + "_" + documentType + "_" + System.currentTimeMillis() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("KYC document saved: {}", filePath);

        return filePath.toString();
    }

    /**
     * Mock KYC verification (for PoC)
     * In production, integrate with real verification service
     */
    private boolean performMockVerification(KycVerificationRequest request, User user) {
        // Simulate verification delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock verification logic
        // For PoC: Accept if document number has even last digit
        String documentNumber = request.getDocumentNumber();
        if (documentNumber != null && !documentNumber.isEmpty()) {
            char lastChar = documentNumber.charAt(documentNumber.length() - 1);
            if (Character.isDigit(lastChar)) {
                int lastDigit = Character.getNumericValue(lastChar);
                boolean approved = lastDigit % 2 == 0;

                log.info("Mock KYC verification for user {}: documentNumber={}, approved={}",
                        user.getId(), maskDocumentNumber(documentNumber), approved);

                return approved;
            }
        }

        // Default: approve if BVN exists (user already has basic verification)
        return user.getBvn() != null && !user.getBvn().isEmpty();
    }

    /**
     * Mask document number for logging (security)
     */
    private String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.length() <= 4) {
            return "****";
        }
        return documentNumber.substring(0, 4) + "****" + documentNumber.substring(documentNumber.length() - 2);
    }

    /**
     * Generate virtual account number for user
     * Delegates to AccountNumberGenerator
     */
    public String generateAccountNumber() {
        return accountNumberGenerator.generateAccountNumber();
    }
}
