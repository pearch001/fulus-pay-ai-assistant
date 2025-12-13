package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for User Profile Management
 *
 * Endpoints:
 * - GET /api/v1/users/profile - Get current user profile
 * - PUT /api/v1/users/profile - Update user profile
 * - GET /api/v1/users/profile/wallet - Get wallet details
 * - POST /api/v1/users/profile/verify-identity - KYC verification
 */
@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final ProfileService profileService;

    /**
     * Get current user profile
     */
    @GetMapping
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("GET /api/v1/users/profile - userId: {}", userPrincipal.getId());

        try {
            UserProfileResponse profile = profileService.getUserProfile(userPrincipal.getId());
            return ResponseEntity.ok(profile);

        } catch (IllegalArgumentException e) {
            log.warn("Get profile failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error getting profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve profile. Please try again."));
        }
    }

    /**
     * Update user profile (name and email only)
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("PUT /api/v1/users/profile - userId: {}", userPrincipal.getId());

        try {
            UserProfileResponse profile = profileService.updateProfile(userPrincipal.getId(), request);
            log.info("Profile updated successfully for user: {}", userPrincipal.getId());
            return ResponseEntity.ok(profile);

        } catch (IllegalArgumentException e) {
            log.warn("Profile update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Profile update failed. Please try again."));
        }
    }

    /**
     * Get wallet details
     */
    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("GET /api/v1/users/profile/wallet - userId: {}", userPrincipal.getId());

        try {
            WalletResponse wallet = profileService.getWallet(userPrincipal.getId());
            return ResponseEntity.ok(wallet);

        } catch (IllegalArgumentException e) {
            log.warn("Get wallet failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error getting wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve wallet. Please try again."));
        }
    }

    /**
     * Mock endpoint: return provided user details (for testing)
     * Example: GET /api/v1/users/profile/mock-details?fullName=Jane%20Doe&dateOfBirth=1990-05-10&phoneNumber=08012345678
     */
    @GetMapping("/mock-details")
    public ResponseEntity<?> getMockUserDetails(
            @RequestParam("fullName") String fullName,
            @RequestParam("dateOfBirth") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam("phoneNumber") String phoneNumber) {

        // Basic validation
        if (fullName == null || fullName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("fullName is required"));
        }
        if (dateOfBirth == null) {
            return ResponseEntity.badRequest().body(createErrorResponse("dateOfBirth is required and must be in ISO format (yyyy-MM-dd)"));
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("phoneNumber is required"));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("fullName", fullName.trim());
        resp.put("dateOfBirth", dateOfBirth);
        resp.put("phoneNumber", phoneNumber.trim());

        return ResponseEntity.ok(resp);
    }

    /**
     * Verify identity with document upload (KYC)
     */
    @PostMapping(value = "/verify-identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyIdentity(
            @RequestParam("documentType") String documentType,
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam(value = "documentFile", required = false) MultipartFile documentFile,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("POST /api/v1/users/profile/verify-identity - userId: {}, documentType: {}",
                userPrincipal.getId(), documentType);

        // Validate document file
        if (documentFile != null && !documentFile.isEmpty()) {
            // Check file size (max 5MB)
            if (documentFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Document file size must not exceed 5MB"));
            }

            // Check file type (images only)
            String contentType = documentFile.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Document must be an image (JPG, PNG) or PDF"));
            }
        }

        try {
            // Create request
            KycVerificationRequest request = new KycVerificationRequest();
            request.setDocumentType(documentType);
            request.setDocumentNumber(documentNumber);

            // Perform verification
            KycVerificationResponse response = profileService.verifyIdentity(
                    userPrincipal.getId(),
                    request,
                    documentFile
            );

            if (response.isSuccess()) {
                log.info("KYC verification successful for user: {}", userPrincipal.getId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("KYC verification failed for user: {}", userPrincipal.getId());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("KYC verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during KYC verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Verification failed. Please try again."));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
