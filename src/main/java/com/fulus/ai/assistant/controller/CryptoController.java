package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.CryptoUtilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for cryptographic operations
 */
@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Crypto", description = "Cryptographic operations API")
@SecurityRequirement(name = "Bearer Authentication")
public class CryptoController {

    private final CryptoUtilityService cryptoUtilityService;

    /**
     * Generate a new key pair for the user
     * This generates keys on the server (for testing/demo purposes)
     * In production, keys should be generated on the client device
     */
    @PostMapping("/generate-keys")
    @Operation(summary = "Generate key pair", description = "Generate a new RSA/ECDSA key pair (for testing)")
    public ResponseEntity<ApiResponse<KeyPairDTO>> generateKeys(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "RSA") String algorithm) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/crypto/generate-keys - User: {}, Algorithm: {}", userId, algorithm);

        try {
            KeyPairDTO keyPair = cryptoUtilityService.generateKeyPair(algorithm);

            // Auto-register the generated public key
            cryptoUtilityService.storePublicKey(userId, keyPair);

            log.info("Key pair generated and registered for user: {}", userId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Key pair generated successfully. IMPORTANT: Store your private key securely!",
                    keyPair));
        } catch (IllegalArgumentException e) {
            log.error("Invalid algorithm: {}", algorithm);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid algorithm: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating keys for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate keys: " + e.getMessage()));
        }
    }

    /**
     * Register a public key for the user
     * The user provides their public key (generated on their device)
     */
    @PostMapping("/register-keys")
    @Operation(summary = "Register public key", description = "Register your public key for offline transactions")
    public ResponseEntity<ApiResponse<String>> registerKeys(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegisterKeysRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/crypto/register-keys - User: {}", userId);

        try {
            // Create KeyPairDTO from request
            KeyPairDTO keyPair = KeyPairDTO.builder()
                    .keyId(UUID.randomUUID().toString())
                    .publicKey(request.getPublicKey())
                    .algorithm(request.getAlgorithm() != null ? request.getAlgorithm() : "RSA")
                    .keySize(request.getKeySize() != null ? request.getKeySize() : 2048)
                    .build();

            // Store the public key
            cryptoUtilityService.storePublicKey(userId, keyPair);

            log.info("Public key registered successfully for user: {}", userId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Public key registered successfully. You can now generate QR codes!",
                    keyPair.getKeyId()));
        } catch (Exception e) {
            log.error("Error registering keys for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register keys: " + e.getMessage()));
        }
    }

    /**
     * Get the user's registered public key
     */
    @GetMapping("/public-key")
    @Operation(summary = "Get public key", description = "Retrieve your registered public key")
    public ResponseEntity<ApiResponse<String>> getPublicKey(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        log.info("GET /api/v1/crypto/public-key - User: {}", userId);

        String publicKey = cryptoUtilityService.getUserPublicKey(userId);

        if (publicKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No public key registered. Please register your keys first."));
        }

        return ResponseEntity.ok(ApiResponse.success("Public key retrieved", publicKey));
    }

    /**
     * Helper method to extract user ID from UserDetails
     */
    private UUID getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal) {
            return ((UserPrincipal) userDetails).getId();
        }
        throw new IllegalStateException("UserDetails is not an instance of UserPrincipal");
    }
}

