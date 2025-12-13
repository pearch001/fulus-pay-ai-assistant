package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.service.UserDeviceService;
import com.fulus.ai.assistant.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for user device management
 * Handles device registration, listing, suspension, and revocation
 */
@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Validated
public class UserDeviceController {

    private final UserDeviceService userDeviceService;
    private final RateLimiter rateLimiter;

    /**
     * 1. Register new device for user
     * POST /api/devices/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                log.warn("Unauthorized device registration attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.info("Device registration request from user: {}, deviceId: {}", userId, request.getDeviceId());

            // Rate limiting check
            String rateLimitKey = "device-register:" + userId;
            if (!rateLimiter.allowRequest(rateLimitKey, 5, 3600)) { // 5 registrations per hour
                log.warn("Rate limit exceeded for device registration: {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before registering more devices.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Register device
            DeviceRegistrationResponse response = userDeviceService.registerDevice(userId, request);

            log.info("Device registered successfully: userId={}, deviceId={}", userId, request.getDeviceId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Device registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("REGISTRATION_FAILED")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error registering device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to register device: " + e.getMessage())
                            .code("REGISTRATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 2. List all devices for current user
     * GET /api/devices/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> listDevices() {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                log.warn("Unauthorized device list request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.debug("Listing devices for user: {}", userId);

            // Get devices
            List<DeviceInfo> devices = userDeviceService.listDevices(userId);

            // Get statistics
            Map<String, Object> statistics = userDeviceService.getDeviceStatistics(userId);

            Map<String, Object> response = Map.of(
                    "devices", devices,
                    "statistics", statistics
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error listing devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to list devices: " + e.getMessage())
                            .code("LIST_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 3. Revoke device (requires PIN confirmation)
     * DELETE /api/devices/{deviceId}/revoke
     */
    @DeleteMapping("/{deviceId}/revoke")
    public ResponseEntity<?> revokeDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody RevokeDeviceRequest request) {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                log.warn("Unauthorized device revocation attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.info("Device revocation request: userId={}, deviceId={}", userId, deviceId);

            // Rate limiting check
            String rateLimitKey = "device-revoke:" + userId;
            if (!rateLimiter.allowRequest(rateLimitKey, 10, 3600)) { // 10 revocations per hour
                log.warn("Rate limit exceeded for device revocation: {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before revoking more devices.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Revoke device (with PIN verification)
            userDeviceService.revokeDevice(userId, deviceId, request.getPin(), request.getReason());

            log.info("Device revoked successfully: deviceId={}", deviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device revoked successfully. All pending transactions from this device have been invalidated.",
                    "deviceId", deviceId,
                    "revokedAt", LocalDateTime.now()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Device revocation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("REVOCATION_FAILED")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (IllegalStateException e) {
            log.error("Device revocation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_STATE")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error revoking device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to revoke device: " + e.getMessage())
                            .code("REVOCATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 4. Suspend device temporarily
     * POST /api/devices/{deviceId}/suspend
     */
    @PostMapping("/{deviceId}/suspend")
    public ResponseEntity<?> suspendDevice(
            @PathVariable String deviceId,
            @RequestBody(required = false) SuspendDeviceRequest request) {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                log.warn("Unauthorized device suspension attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.info("Device suspension request: userId={}, deviceId={}", userId, deviceId);

            // Rate limiting check
            String rateLimitKey = "device-suspend:" + userId;
            if (!rateLimiter.allowRequest(rateLimitKey, 20, 3600)) { // 20 suspensions per hour
                log.warn("Rate limit exceeded for device suspension: {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before suspending more devices.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Suspend device
            String reason = (request != null && request.getReason() != null) ? request.getReason() : "Suspended by user";
            userDeviceService.suspendDevice(userId, deviceId, reason);

            log.info("Device suspended successfully: deviceId={}", deviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device suspended successfully. This device cannot sync transactions until reactivated.",
                    "deviceId", deviceId,
                    "suspendedAt", LocalDateTime.now()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Device suspension failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("SUSPENSION_FAILED")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (IllegalStateException e) {
            log.error("Device suspension failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_STATE")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error suspending device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to suspend device: " + e.getMessage())
                            .code("SUSPENSION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Bonus: Reactivate suspended device
     * POST /api/devices/{deviceId}/reactivate
     */
    @PostMapping("/{deviceId}/reactivate")
    public ResponseEntity<?> reactivateDevice(@PathVariable String deviceId) {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                log.warn("Unauthorized device reactivation attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.info("Device reactivation request: userId={}, deviceId={}", userId, deviceId);

            // Reactivate device
            userDeviceService.reactivateDevice(userId, deviceId);

            log.info("Device reactivated successfully: deviceId={}", deviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device reactivated successfully. This device can now sync transactions.",
                    "deviceId", deviceId,
                    "reactivatedAt", LocalDateTime.now()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Device reactivation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("REACTIVATION_FAILED")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (IllegalStateException e) {
            log.error("Device reactivation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_STATE")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error reactivating device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to reactivate device: " + e.getMessage())
                            .code("REACTIVATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Get device statistics
     * GET /api/devices/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            // Get authenticated user ID
            String userId = getUserIdFromAuthentication();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            Map<String, Object> statistics = userDeviceService.getDeviceStatistics(userId);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error getting device statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to get statistics")
                            .code("STATISTICS_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Get user ID from authentication context
     */
    private String getUserIdFromAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    return (String) principal;
                }
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not extract user ID from authentication", e);
        }
        return null;
    }

    /**
     * Error response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private String code;
        private LocalDateTime timestamp;
    }
}
