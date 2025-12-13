package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Authentication
 *
 * Endpoints:
 * - POST /api/v1/auth/register - Register new user
 * - POST /api/v1/auth/login - Login with password
 * - POST /api/v1/auth/refresh - Refresh access token
 * - POST /api/v1/auth/logout - Logout (revoke refresh tokens)
 * - PUT /api/v1/auth/change-pin - Change PIN (requires authentication)
 * - POST /api/v1/auth/forgot-pin - Initiate PIN reset (forgot PIN)
 * - POST /api/v1/auth/reset-pin - Reset PIN with OTP
 * - POST /api/v1/auth/create-pin - Create PIN after signup/login
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register - phoneNumber: {}", request.getPhoneNumber());

        try {
            AuthResponse response = authenticationService.register(request);
            log.info("User registered successfully: {}", request.getPhoneNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed. Please try again."));
        }
    }

    /**
     * Login with phone number and password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login - phoneNumber: {}", request.getPhoneNumber());

        try {
            AuthResponse response = authenticationService.login(request);
            log.info("User logged in successfully: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed: Invalid credentials for {}", request.getPhoneNumber());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid phone number or password"));

        } catch (LockedException e) {
            log.warn("Login failed: Account locked for {}", request.getPhoneNumber());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed. Please try again."));
        }
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/v1/auth/refresh");

        try {
            AuthResponse response = authenticationService.refreshToken(request.getRefreshToken());
            log.info("Token refreshed successfully");
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid or expired refresh token"));

        } catch (Exception e) {
            log.error("Error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Token refresh failed. Please try again."));
        }
    }

    /**
     * Logout (revoke refresh tokens)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("POST /api/v1/auth/logout - userId: {}", userPrincipal.getId());

        try {
            authenticationService.logout(userPrincipal.getId());
            log.info("User logged out successfully: {}", userPrincipal.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout failed. Please try again."));
        }
    }



    /**
     * Create PIN after signup/login (requires authentication)
     */
    @PostMapping("/create-pin")
    public ResponseEntity<?> createPin(@Valid @RequestBody PinCreateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("POST /api/v1/auth/create-pin - userId: {}", userPrincipal.getId());

        try {
            PinChangeResponse response = authenticationService.createPin(userPrincipal.getId(), request);
            log.info("PIN created successfully for user: {}", userPrincipal.getId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Create PIN failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during create PIN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Create PIN failed. Please try again."));
        }
    }

    /**
     * Change PIN (requires authentication)
     */
    @PutMapping("/change-pin")
    public ResponseEntity<?> changePin(
            @Valid @RequestBody ChangePinRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        log.info("PUT /api/v1/auth/change-pin - userId: {}", userPrincipal.getId());

        try {
            PinChangeResponse response = authenticationService.changePin(userPrincipal.getId(), request);
            log.info("PIN changed successfully for user: {}", userPrincipal.getId());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("PIN change failed: Invalid old PIN for user {}", userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("PIN change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during PIN change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PIN change failed. Please try again."));
        }
    }

    /**
     * Forgot PIN - Initiate PIN reset with BVN verification
     */
    @PostMapping("/forgot-pin")
    public ResponseEntity<?> forgotPin(@Valid @RequestBody ForgotPinRequest request) {
        log.info("POST /api/v1/auth/forgot-pin - phoneNumber: {}", request.getPhoneNumber());

        try {
            ForgotPinResponse response = authenticationService.forgotPin(request);
            log.info("PIN reset OTP generated for phone: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Forgot PIN failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during forgot PIN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PIN reset request failed. Please try again."));
        }
    }

    /**
     * Reset PIN with OTP
     */
    @PostMapping("/reset-pin")
    public ResponseEntity<?> resetPin(@Valid @RequestBody ResetPinRequest request) {
        log.info("POST /api/v1/auth/reset-pin");

        try {
            PinChangeResponse response = authenticationService.resetPin(request);
            log.info("PIN reset successfully");
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("PIN reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("PIN reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error during PIN reset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PIN reset failed. Please try again."));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication Service is running");
    }

    /**
     * Mock endpoint: return provided user details (for testing)
     * Example: GET /api/v1/auth/mock-details?fullName=Jane%20Doe&dateOfBirth=1990-05-10&phoneNumber=08012345678
     */
    @GetMapping("/mock-verify-bvn")
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
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
