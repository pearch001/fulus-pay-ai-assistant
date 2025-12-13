package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.Card;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.CardStatus;
import com.fulus.ai.assistant.enums.CardType;
import com.fulus.ai.assistant.repository.CardRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for card management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    /**
     * Get all cards for a user
     */
    public List<CardDTO> getUserCards(UUID userId) {
        log.info("Fetching cards for user: {}", userId);

        return cardRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get card details (requires PIN)
     */
    @Transactional
    public CardActionResponse getCardDetails(UUID userId, UUID cardId, CardActionRequest request) {
        log.info("Get card details request for card: {} by user: {}", cardId, userId);

        // Verify user PIN
        verifyUserPin(userId, request.getPin());

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Verify card belongs to user
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to card");
        }

        // Build card details
        CardDetailsDTO details = CardDetailsDTO.builder()
                .cardNumber(card.getCardNumber())
                .cardHolderName(card.getCardHolderName())
                .expiryDate(card.getExpiryDate())
                .cvv(card.getCvv())
                .build();

        return CardActionResponse.success("Card details retrieved successfully", details);
    }

    /**
     * Set transaction limits (requires PIN)
     */
    @Transactional
    public CardActionResponse setTransactionLimit(UUID userId, UUID cardId, SetCardLimitRequest request) {
        log.info("Set transaction limit request for card: {} by user: {}", cardId, userId);

        // Verify user PIN
        verifyUserPin(userId, request.getPin());

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Verify card belongs to user
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to card");
        }

        // Validate limits
        if (request.getTransactionLimit() > request.getDailyLimit()) {
            throw new IllegalArgumentException("Transaction limit cannot exceed daily limit");
        }

        // Update limits
        card.setDailyLimit(BigDecimal.valueOf(request.getDailyLimit()));
        card.setTransactionLimit(BigDecimal.valueOf(request.getTransactionLimit()));
        cardRepository.save(card);

        log.info("Transaction limits updated for card: {}", cardId);
        return CardActionResponse.success("Transaction limits updated successfully");
    }

    /**
     * Change card PIN (requires old PIN and account PIN)
     */
    @Transactional
    public CardActionResponse changeCardPin(UUID userId, UUID cardId, ChangeCardPinRequest request) {
        log.info("Change card PIN request for card: {} by user: {}", cardId, userId);

        // Verify account PIN
        verifyUserPin(userId, request.getAccountPin());

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Verify card belongs to user
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to card");
        }

        // Verify old card PIN
        if (!passwordEncoder.matches(request.getOldPin(), card.getCardPin())) {
            log.warn("SECURITY: Invalid old card PIN for card: {}", cardId);
            throw new IllegalArgumentException("Invalid old card PIN");
        }

        // Validate new PIN (should not be same as old)
        if (request.getOldPin().equals(request.getNewPin())) {
            throw new IllegalArgumentException("New PIN must be different from old PIN");
        }

        // Update card PIN
        card.setCardPin(passwordEncoder.encode(request.getNewPin()));
        cardRepository.save(card);

        log.info("SECURITY: Card PIN changed successfully for card: {}", cardId);
        return CardActionResponse.success("Card PIN changed successfully");
    }

    /**
     * Toggle SMS alert subscription (requires PIN)
     */
    @Transactional
    public CardActionResponse toggleSmsAlert(UUID userId, UUID cardId, CardActionRequest request) {
        log.info("Toggle SMS alert request for card: {} by user: {}", cardId, userId);

        // Verify user PIN
        verifyUserPin(userId, request.getPin());

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Verify card belongs to user
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to card");
        }

        // Toggle SMS alert
        boolean newStatus = !card.getSmsAlertEnabled();
        card.setSmsAlertEnabled(newStatus);
        cardRepository.save(card);

        String message = newStatus ? "SMS alerts enabled" : "SMS alerts disabled";
        log.info("{} for card: {}", message, cardId);
        return CardActionResponse.success(message);
    }

    /**
     * Block card (requires PIN)
     */
    @Transactional
    public CardActionResponse blockCard(UUID userId, UUID cardId, BlockCardRequest request) {
        log.info("Block card request for card: {} by user: {}", cardId, userId);

        // Verify user PIN
        verifyUserPin(userId, request.getPin());

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Verify card belongs to user
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to card");
        }

        // Check if already blocked
        if (card.isBlocked()) {
            throw new IllegalArgumentException("Card is already blocked");
        }

        // Block card
        card.block(request.getReason());
        cardRepository.save(card);

        log.warn("SECURITY: Card blocked - ID: {}, Reason: {}", cardId, request.getReason());
        return CardActionResponse.success("Card blocked successfully");
    }

    /**
     * Create a virtual card for user (auto-created during registration)
     */
    @Transactional
    public Card createVirtualCard(User user) {
        log.info("Creating virtual card for user: {}", user.getId());

        // Check if virtual card already exists
        if (cardRepository.existsByUserIdAndCardType(user.getId(), CardType.VIRTUAL)) {
            log.warn("Virtual card already exists for user: {}", user.getId());
            return cardRepository.findByUserIdAndCardType(user.getId(), CardType.VIRTUAL)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        // Generate card details
        String cardNumber = generateCardNumber();
        String cvv = generateCVV();
        LocalDate expiryDate = LocalDate.now().plusYears(3);

        // Create virtual card
        Card card = Card.builder()
                .userId(user.getId())
                .cardType(CardType.VIRTUAL)
                .cardNumber(cardNumber)
                .cardHolderName(user.getName().toUpperCase())
                .expiryDate(expiryDate)
                .cvv(cvv)
                .cardPin(passwordEncoder.encode("0000")) // Default PIN
                .status(CardStatus.ACTIVE)
                .dailyLimit(new BigDecimal("500000.00"))
                .transactionLimit(new BigDecimal("100000.00"))
                .smsAlertEnabled(false)
                .internationalTransactionsEnabled(false)
                .onlineTransactionsEnabled(true)
                .contactlessEnabled(true)
                .build();

        card.activate();
        card = cardRepository.save(card);

        log.info("Virtual card created successfully for user: {}", user.getId());
        return card;
    }

    /**
     * Create a physical card for a user. If one already exists, returns it.
     * Physical cards are created with status PENDING so they can be produced/activated later.
     */
    @Transactional
    public Card createPhysicalCard(User user) {
        log.info("Creating physical card request for user: {}", user.getId());

        // If a physical card already exists, return it
        if (cardRepository.existsByUserIdAndCardType(user.getId(), CardType.PHYSICAL)) {
            log.warn("Physical card already exists for user: {}", user.getId());
            return cardRepository.findByUserIdAndCardType(user.getId(), CardType.PHYSICAL)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        // Generate card details
        String cardNumber = generateCardNumber();
        String cvv = generateCVV();
        LocalDate expiryDate = LocalDate.now().plusYears(5); // longer expiry for physical card

        // Build physical card with PENDING status (to be produced/activated later)
        Card card = Card.builder()
                .userId(user.getId())
                .cardType(CardType.PHYSICAL)
                .cardNumber(cardNumber)
                .cardHolderName(user.getName().toUpperCase())
                .expiryDate(expiryDate)
                .cvv(cvv)
                .cardPin(passwordEncoder.encode("0000")) // Default PIN, user must change after activation
                .status(CardStatus.PENDING_ACTIVATION)
                .dailyLimit(new BigDecimal("500000.00"))
                .transactionLimit(new BigDecimal("100000.00"))
                .smsAlertEnabled(false)
                .internationalTransactionsEnabled(true)
                .onlineTransactionsEnabled(true)
                .contactlessEnabled(true)
                .build();

        card = cardRepository.save(card);

        log.info("Physical card request created for user: {} (card id: {})", user.getId(), card.getId());
        return card;
    }

    /**
     * Verify user PIN
     */
    private User verifyUserPin(UUID userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPin() == null || user.getPin().isEmpty()) {
            throw new IllegalArgumentException("Please set up your account PIN first");
        }

        if (!passwordEncoder.matches(pin, user.getPin())) {
            log.warn("SECURITY: Invalid PIN attempt for user: {}", userId);
            throw new IllegalArgumentException("Invalid PIN");
        }

        return user;
    }

    /**
     * Map Card entity to DTO
     */
    private CardDTO mapToDTO(Card card) {
        return CardDTO.builder()
                .id(card.getId())
                .cardType(card.getCardType())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .dailyLimit(card.getDailyLimit())
                .transactionLimit(card.getTransactionLimit())
                .smsAlertEnabled(card.getSmsAlertEnabled())
                .internationalTransactionsEnabled(card.getInternationalTransactionsEnabled())
                .onlineTransactionsEnabled(card.getOnlineTransactionsEnabled())
                .contactlessEnabled(card.getContactlessEnabled())
                .createdAt(card.getCreatedAt())
                .activatedAt(card.getActivatedAt())
                .build();
    }

    /**
     * Generate random card number (dummy)
     */
    private String generateCardNumber() {
        // Generate a 16-digit card number starting with 5399 (dummy BIN)
        StringBuilder cardNumber = new StringBuilder("5399");
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
    }

    /**
     * Generate random CVV (dummy)
     */
    private String generateCVV() {
        return String.format("%03d", random.nextInt(1000));
    }
}
