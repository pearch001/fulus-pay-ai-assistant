package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.security.UserPrincipal;
import com.fulus.ai.assistant.service.BankService;
import com.fulus.ai.assistant.service.TransferService;
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
 * Controller for handling transfers (internal and inter-bank)
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transfers", description = "Transfer operations API")
@SecurityRequirement(name = "Bearer Authentication")
public class TransferController {

    private final TransferService transferService;
    private final BankService bankService;

    /**
     * Get list of all banks
     */
    @GetMapping("/banks")
    @Operation(summary = "Get bank list", description = "Retrieve list of all supported banks")
    public ResponseEntity<BankListResponse> getBankList() {
        log.info("GET /api/v1/transfers/banks - Bank list request");
        BankListResponse response = bankService.getBankList();
        return ResponseEntity.ok(response);
    }

    /**
     * Name enquiry for account verification
     */
    @PostMapping("/name-enquiry")
    @Operation(summary = "Name enquiry", description = "Verify account name before transfer")
    public ResponseEntity<NameEnquiryResponse> nameEnquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NameEnquiryRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/transfers/name-enquiry - User: {}, Account: {}, Bank: {}",
                userId, request.getAccountNumber(), request.getBankCode());

        try {
            NameEnquiryResponse response = bankService.nameEnquiry(request, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Name enquiry failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(NameEnquiryResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error during name enquiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NameEnquiryResponse.failure("An error occurred during verification"));
        }
    }

    /**
     * Internal transfer (between Fulus Pay users)
     */
    @PostMapping("/internal")
    @Operation(summary = "Internal transfer", description = "Transfer to another Fulus Pay user")
    public ResponseEntity<TransferResponse> internalTransfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InternalTransferRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/transfers/internal - User: {}, Recipient: {}, Amount: ₦{}",
                userId, request.getRecipientIdentifier(), request.getAmount());

        try {
            TransferResponse response = transferService.internalTransfer(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Internal transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(TransferResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error during internal transfer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TransferResponse.failure("An error occurred during transfer"));
        }
    }

    /**
     * Inter-bank transfer (to external banks)
     */
    @PostMapping("/inter-bank")
    @Operation(summary = "Inter-bank transfer", description = "Transfer to external bank account")
    public ResponseEntity<TransferResponse> interBankTransfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InterBankTransferRequest request) {

        UUID userId = getUserId(userDetails);
        log.info("POST /api/v1/transfers/inter-bank - User: {}, Account: {}, Bank: {}, Amount: ₦{}",
                userId, request.getAccountNumber(), request.getBankCode(), request.getAmount());

        try {
            TransferResponse response = transferService.interBankTransfer(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Inter-bank transfer failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(TransferResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error during inter-bank transfer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TransferResponse.failure("An error occurred during transfer"));
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
