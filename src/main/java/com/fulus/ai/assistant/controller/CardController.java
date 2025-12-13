package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.CardService;
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

import java.util.List;
import java.util.UUID;

/**
 * Controller for card management operations
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cards", description = "Card management API")
@SecurityRequirement(name = "Bearer Authentication")
public class CardController {

    private final CardService cardService;

    /**
     * Get all cards for authenticated user
     */
    @GetMapping
    @Operation(summary = "Get user cards", description = "Retrieve all cards for the authenticated user")
    public ResponseEntity<List<CardDTO>> getUserCards(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        log.info("GET /api/v1/cards - User: {}", userId);

        List<CardDTO> cards = cardService.getUserCards(userId);
        return ResponseEntity.ok(cards);
    }

    /**
     * Get full card details (requires PIN)
     */
    @PostMapping("/{cardId}/details")
    @Operation(summary = "View card details", description = "View full card details including card number and CVV (requires PIN)")
    public ResponseEntity<CardActionResponse> getCardDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody CardActionRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/cards/{}/details - User: {}", cardId, userId);

        try {
            CardActionResponse response = cardService.getCardDetails(userId, cardId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Get card details failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CardActionResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting card details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CardActionResponse.failure("An error occurred"));
        }
    }

    /**
     * Set transaction limits (requires PIN)
     */
    @PutMapping("/{cardId}/limits")
    @Operation(summary = "Set transaction limits", description = "Set daily and per-transaction limits (requires PIN)")
    public ResponseEntity<CardActionResponse> setTransactionLimit(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody SetCardLimitRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("PUT /api/v1/cards/{}/limits - User: {}", cardId, userId);

        try {
            CardActionResponse response = cardService.setTransactionLimit(userId, cardId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Set transaction limit failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CardActionResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting transaction limit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CardActionResponse.failure("An error occurred"));
        }
    }

    /**
     * Change card PIN (requires old PIN and account PIN)
     */
    @PutMapping("/{cardId}/pin")
    @Operation(summary = "Change card PIN", description = "Change card PIN (requires old card PIN and account PIN)")
    public ResponseEntity<CardActionResponse> changeCardPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody ChangeCardPinRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("PUT /api/v1/cards/{}/pin - User: {}", cardId, userId);

        try {
            CardActionResponse response = cardService.changeCardPin(userId, cardId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Change card PIN failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CardActionResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing card PIN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CardActionResponse.failure("An error occurred"));
        }
    }

    /**
     * Toggle SMS alert subscription (requires PIN)
     */
    @PostMapping("/{cardId}/sms-alerts/toggle")
    @Operation(summary = "Toggle SMS alerts", description = "Enable or disable SMS alerts for card transactions (requires PIN)")
    public ResponseEntity<CardActionResponse> toggleSmsAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody CardActionRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/cards/{}/sms-alerts/toggle - User: {}", cardId, userId);

        try {
            CardActionResponse response = cardService.toggleSmsAlert(userId, cardId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Toggle SMS alert failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CardActionResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling SMS alert", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CardActionResponse.failure("An error occurred"));
        }
    }

    /**
     * Block card (requires PIN)
     */
    @PostMapping("/{cardId}/block")
    @Operation(summary = "Block card", description = "Block card (requires PIN)")
    public ResponseEntity<CardActionResponse> blockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody BlockCardRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/cards/{}/block - User: {}", cardId, userId);

        try {
            CardActionResponse response = cardService.blockCard(userId, cardId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Block card failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CardActionResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error blocking card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CardActionResponse.failure("An error occurred"));
        }
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
