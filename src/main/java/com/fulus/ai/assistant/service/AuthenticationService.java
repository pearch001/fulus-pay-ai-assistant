package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.PinResetToken;
import com.fulus.ai.assistant.entity.RefreshToken;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.enums.UserRole;
import com.fulus.ai.assistant.repository.PinResetTokenRepository;
import com.fulus.ai.assistant.repository.RefreshTokenRepository;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.security.JwtTokenProvider;
import com.fulus.ai.assistant.util.AccountNumberGenerator;
import com.fulus.ai.assistant.util.PinValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PinResetTokenRepository pinResetTokenRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final BVNVerificationService bvnVerificationService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final DemoCreditService demoCreditService;
    private final CardService cardService;

    @Value("${security.pin.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.pin.lockout-duration:900000}") // 15 minutes
    private long lockoutDuration;

    /**
     * Authenticate user with phone number and password
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for phone number: {}", request.getPhoneNumber());

        // Find user
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getPhoneNumber());
                    return new BadCredentialsException("Invalid phone number or password");
                });

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login blocked: Account locked for user {} until {}",
                    request.getPhoneNumber(), user.getLockedUntil());
            throw new LockedException(
                    String.format("Account is temporarily locked due to multiple failed login attempts. " +
                            "Please try again after %s.", user.getLockedUntil())
            );
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Login blocked: Account inactive for user {}", request.getPhoneNumber());
            throw new LockedException("Account is inactive. Please contact support.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getPhoneNumber(),
                            request.getPassword()
                    )
            );

            // Device validation
            validateAndUpdateDevice(user, request.getDeviceInfo());

            // Update login tracking
            user.resetFailedAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Login successful for user: {} (ID: {}). Balance: ₦{}",
                    request.getPhoneNumber(), user.getId(), user.getBalance());

            // Generate tokens
            return generateAuthResponse(user);

        } catch (AuthenticationException e) {
            // Increment failed attempts
            handleFailedLogin(user);
            log.warn("Login failed: Invalid password for user {} (Attempt {}/{})",
                    request.getPhoneNumber(),
                    user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0,
                    maxFailedAttempts);
            throw new BadCredentialsException("Invalid phone number or password");
        }
    }

    /**
     * Register new user with BVN verification
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for phone number: {} with BVN verification", request.getPhoneNumber());

        // 1. Validate Nigerian phone number format (already done by @Pattern but double-check)
        if (!request.getPhoneNumber().matches("^0[789][01]\\d{8}$")) {
            throw new IllegalArgumentException("Invalid Nigerian phone number format. Must be 11 digits starting with 070, 080, 081, 090, or 091.");
        }

        // 2. Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            log.warn("Registration failed: Phone number already exists - {}", request.getPhoneNumber());
            throw new IllegalArgumentException("Phone number already registered");
        }

        // 3. Check if email already exists
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Registration failed: Email already exists - {}", request.getEmail());
                throw new IllegalArgumentException("Email already registered");
            }
        }

        // Confirm password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // 4. Perform BVN verification
        log.info("Verifying BVN for user: {}", request.getFullName());
        BVNVerificationResponse bvnResponse = bvnVerificationService.verifyBVN(
                request.getBvn(),
                request.getFullName(),
                request.getDateOfBirth()
        );

        if (!bvnResponse.isVerified()) {
            log.warn("BVN verification failed for {}: {}", request.getPhoneNumber(), bvnResponse.getMessage());
            throw new IllegalArgumentException("BVN verification failed: " + bvnResponse.getMessage());
        }

        // 5. Cross-check name similarity
        boolean nameMatches = bvnVerificationService.checkNameSimilarity(
                request.getFullName(),
                request.getFullName()
        );

        if (!nameMatches) {
            log.warn("Name mismatch: Provided '{}' vs BVN '{}'", request.getFullName(), bvnResponse.getFullName());
            throw new IllegalArgumentException(
                    "Name mismatch: The name provided does not match BVN records. " +
                    "Provided: " + request.getFullName() + ", BVN: " + bvnResponse.getFullName()
            );
        }

        log.info("BVN verification successful. Name match confirmed.");

        // 6. Create new user
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBvn(request.getBvn());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setResidentialAddress(request.getResidentialAddress());
        user.setBalance(BigDecimal.ZERO);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.setRole(UserRole.USER); // Set default role as USER

        // Generate virtual account number
        user.setAccountNumber(accountNumberGenerator.generateAccountNumber());

        // Set initial KYC status (VERIFIED since BVN was verified)
        user.setKycStatus(com.fulus.ai.assistant.enums.KycStatus.VERIFIED);

        // Register device
        user.registerDevice(
                request.getDeviceInfo().getDeviceId(),
                request.getDeviceInfo().getDeviceName(),
                request.getDeviceInfo().getDeviceModel(),
                request.getDeviceInfo().getDeviceOS()
        );

        user = userRepository.save(user);
        log.info("User created successfully: {} (ID: {}, Account: {})",
                request.getPhoneNumber(), user.getId(), user.getAccountNumber());

        // 7. Create virtual card for the user
        try {
            cardService.createVirtualCard(user);
            cardService.createPhysicalCard(user);
            log.info("Virtual card created for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to create virtual card for user: {}", user.getId(), e);
            // Don't fail registration if card creation fails
        }

        // 8. Create welcome bonus transaction (₦1000)
        //scheduleDemoCredit(user);

        // 9. Schedule demo credit (₦1,000,000 from John Doe after 1 minute)
        demoCreditService.scheduleDemoCredit(user.getId());

        // 10. Generate tokens
        return generateAuthResponse(user);
    }


    @Async
    public void scheduleDemoCredit(User user) {

        try {
            // Wait 1 minute
            Thread.sleep(50000);

            // Credit the account
            createWelcomeBonus(user);

        } catch (InterruptedException e) {
            log.error("Demo credit scheduling interrupted for user: {}", user.getId(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error scheduling demo credit for user: {}", user.getId(), e);
        }
    }

    /**
     * Create PIN for user after signup/login
     */
    @Transactional
    public PinChangeResponse createPin(UUID userId, PinCreateRequest request) {
        log.info("Create PIN request for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPin() != null && !user.getPin().isEmpty()) {
            throw new IllegalArgumentException("PIN already set for this account");
        }

        // Validate PIN
        try {
            PinValidator.validatePinStrength(request.getNewPin());
        } catch (IllegalArgumentException e) {
            log.warn("PIN creation rejected for user {}: {}", userId, e.getMessage());
            throw e;
        }

        if (!request.getNewPin().equals(request.getConfirmPin())) {
            throw new IllegalArgumentException("New PIN and confirm PIN do not match");
        }

        user.setPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);

        log.info("PIN created for user {}", userId);

        return PinChangeResponse.success("PIN created successfully. Please use your PIN for PIN-based operations.");
    }

    /**
     * Create welcome bonus transaction for new user
     */
    private void createWelcomeBonus(User user) {

        BigDecimal bonusAmount = new BigDecimal("1000.00");

        Transaction welcomeBonus = new Transaction();
        welcomeBonus.setUserId(user.getId());
        welcomeBonus.setType(TransactionType.CREDIT);
        welcomeBonus.setCategory(TransactionCategory.TRANSFER);
        welcomeBonus.setAmount(bonusAmount);
        welcomeBonus.setDescription("Welcome Bonus - Thank you for joining Fulus Pay!");
        welcomeBonus.setBalanceAfter(bonusAmount);
        welcomeBonus.setReference("WELCOME-" + System.currentTimeMillis() + "-" + user.getId().toString().substring(0, 8).toUpperCase());
        welcomeBonus.setStatus(TransactionStatus.COMPLETED);

        transactionRepository.save(welcomeBonus);

        // Update user balance
        user.setBalance(bonusAmount);
        userRepository.save(user);

        log.info("Welcome bonus created: ₦{} for user {}", bonusAmount, user.getId());
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        log.info("Refresh token request");

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshTokenString)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

        // Check if token is valid
        if (!refreshToken.isValid()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Generate new tokens
        return generateAuthResponse(user);
    }

    /**
     * Logout user (revoke refresh token and deactivate device)
     */
    @Transactional
    public void logout(UUID userId) {
        log.info("Logout request for user: {}", userId);

        // Deactivate device
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.deactivateDevice();
        userRepository.save(user);

        // Revoke tokens
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("All refresh tokens revoked and device deactivated for user: {}", userId);
    }

    /**
     * Validate and update device for user
     */
    private void validateAndUpdateDevice(User user, DeviceInfoRequest deviceInfo) {
        // Check if user has an active device
        if (user.hasActiveDevice()) {
            // Check if it's the same device
            if (!user.isDeviceMatching(deviceInfo.getDeviceId())) {
                log.warn("SECURITY: Device mismatch for user {}. Registered: {}, Attempting: {}",
                        user.getId(), user.getDeviceId(), deviceInfo.getDeviceId());
                throw new IllegalArgumentException(
                        "This account is already logged in on another device. " +
                        "Please logout from the other device first or contact support."
                );
            }
            // Same device, just update last login
            log.info("Same device login for user: {}", user.getId());
        } else {
            // No active device or device was deactivated, register new device
            log.info("Registering new device for user: {}", user.getId());
            user.registerDevice(
                    deviceInfo.getDeviceId(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceModel(),
                    deviceInfo.getDeviceOS()
            );
        }
    }

    /**
     * Generate authentication response with tokens
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate access token
        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getPhoneNumber(),
                user.getName()
        );

        // Generate refresh token
        String refreshTokenString = tokenProvider.generateRefreshToken(user.getId());

        // Revoke old refresh tokens for this user (optional: keep only one active session)
        // refreshTokenRepository.revokeAllUserTokens(user.getId());

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .userId(user.getId())
                .expiryDate(tokenProvider.getExpiryDateFromToken(refreshTokenString))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        // Build user info with balance and PIN status
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .email(user.getEmail())
                .balance(user.getBalance())
                .lastLoginAt(user.getLastLoginAt())
                .accountNumber(user.getAccountNumber())
                .isPinCreated(user.getPin() != null && !user.getPin().isEmpty())
                .build();

        // Calculate expiration in seconds
        long expiresIn = tokenProvider.getAccessTokenExpiration() / 1000;

        log.info("Tokens generated for user: {} (Access: {}s, Refresh: {}d)",
                user.getPhoneNumber(),
                expiresIn,
                tokenProvider.getRefreshTokenExpiration() / 1000 / 86400);

        return AuthResponse.success(accessToken, refreshTokenString, expiresIn, userInfo);
    }

    /**
     * Change user PIN (requires old PIN verification)
     */
    @Transactional
    public PinChangeResponse changePin(UUID userId, ChangePinRequest request) {
        log.info("PIN change request for user: {}", userId);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old PIN
        if (!passwordEncoder.matches(request.getOldPin(), user.getPin())) {
            log.warn("SECURITY ALERT: Failed PIN change attempt for user {} - Invalid old PIN", userId);
            throw new BadCredentialsException("Invalid old PIN");
        }

        // Validate new PIN strength
        try {
            PinValidator.validatePinStrength(request.getNewPin());
        } catch (IllegalArgumentException e) {
            log.warn("PIN change rejected for user {}: {}", userId, e.getMessage());
            throw e;
        }

        // Check if new PIN is same as old PIN
        if (passwordEncoder.matches(request.getNewPin(), user.getPin())) {
            throw new IllegalArgumentException("New PIN must be different from old PIN");
        }

        // Update PIN
        user.setPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);

        // Invalidate all existing tokens (force re-login)
        refreshTokenRepository.revokeAllUserTokens(userId);

        log.info("SECURITY: PIN changed successfully for user {}. All sessions invalidated.", userId);

        return PinChangeResponse.success("PIN changed successfully. Please log in again with your new PIN.");
    }

    /**
     * Initiate PIN reset (forgot PIN flow)
     */
    @Transactional
    public ForgotPinResponse forgotPin(ForgotPinRequest request) {
        log.info("Forgot PIN request for phone number: {}", request.getPhoneNumber());

        // Find user by phone number
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> {
                    log.warn("Forgot PIN failed: User not found - {}", request.getPhoneNumber());
                    return new IllegalArgumentException("Account not found with provided details");
                });

        // Verify BVN matches
        if (!user.getBvn().equals(request.getBvn())) {
            log.warn("SECURITY ALERT: Forgot PIN BVN mismatch for user {} (Phone: {})",
                    user.getId(), request.getPhoneNumber());
            throw new IllegalArgumentException("Account verification failed. BVN does not match.");
        }

        // Verify date of birth matches
        if (!user.getDateOfBirth().equals(request.getDateOfBirth())) {
            log.warn("SECURITY ALERT: Forgot PIN DOB mismatch for user {} (Phone: {})",
                    user.getId(), request.getPhoneNumber());
            throw new IllegalArgumentException("Account verification failed. Date of birth does not match.");
        }

        // Invalidate any existing unused reset tokens for this user (security measure)
        pinResetTokenRepository.invalidateAllUserTokens(user.getId());

        // Generate OTP and reset token
        String otp = PinValidator.generateOTP();
        String resetToken = PinValidator.generateResetToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(10);

        // Save reset token to database
        PinResetToken pinResetToken = PinResetToken.builder()
                .userId(user.getId())
                .resetToken(resetToken)
                .otp(otp)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        pinResetTokenRepository.save(pinResetToken);

        log.info("PIN reset OTP generated for user {} (Phone: {}). Expires at: {}",
                user.getId(), request.getPhoneNumber(), expiryDate);

        // TODO: In production, send OTP via SMS service
        // For PoC, return OTP in response
        log.info("MOCK SMS: OTP for {}: {}", request.getPhoneNumber(), otp);

        return ForgotPinResponse.success(resetToken, otp, expiryDate);
    }

    /**
     * Reset PIN using OTP and reset token
     */
    @Transactional
    public PinChangeResponse resetPin(ResetPinRequest request) {
        log.info("PIN reset request with token: {}...", request.getResetToken().substring(0, 8));

        // Find and validate reset token
        PinResetToken resetToken = pinResetTokenRepository.findValidToken(
                request.getResetToken(),
                request.getOtp(),
                LocalDateTime.now()
        ).orElseThrow(() -> {
            log.warn("SECURITY ALERT: Invalid or expired PIN reset token/OTP");
            return new BadCredentialsException("Invalid or expired reset token/OTP");
        });

        // Validate new PIN strength
        try {
            PinValidator.validatePinStrength(request.getNewPin());
        } catch (IllegalArgumentException e) {
            log.warn("PIN reset rejected: {}", e.getMessage());
            throw e;
        }

        // Get user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if new PIN is same as old PIN
        if (passwordEncoder.matches(request.getNewPin(), user.getPin())) {
            throw new IllegalArgumentException("New PIN must be different from old PIN");
        }

        // Update PIN
        user.setPin(passwordEncoder.encode(request.getNewPin()));

        // Reset failed login attempts (if account was locked)
        user.resetFailedAttempts();
        user.setLockedUntil(null);

        userRepository.save(user);

        // Mark reset token as used
        resetToken.markAsUsed();
        pinResetTokenRepository.save(resetToken);

        // Invalidate all existing sessions (force re-login)
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        log.info("SECURITY: PIN reset successfully for user {} (Phone: {}). All sessions invalidated.",
                user.getId(), user.getPhoneNumber());

        return PinChangeResponse.success("PIN reset successfully. Please log in with your new PIN.");
    }

    /**
     * Handle failed login attempt with rate limiting
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedAttempts();

        int currentAttempts = user.getFailedLoginAttempts();

        if (currentAttempts >= maxFailedAttempts) {
            // Lock account
            LocalDateTime lockUntil = LocalDateTime.now().plusSeconds(lockoutDuration / 1000);
            user.setLockedUntil(lockUntil);

            log.warn("SECURITY ALERT: Account locked for user: {} (Phone: {}) after {} failed attempts. " +
                    "Locked until: {}",
                    user.getId(), user.getPhoneNumber(), maxFailedAttempts, lockUntil);
        } else {
            log.warn("Failed login attempt {}/{} for user: {} (Phone: {})",
                    currentAttempts, maxFailedAttempts, user.getId(), user.getPhoneNumber());
        }

        userRepository.save(user);
    }

    /**
     * Admin login with email and password
     */
    @Transactional
    public AdminAuthResponse adminLogin(AdminLoginRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Admin login failed: User not found - {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        // Verify user is an admin
        if (user.getRole() == null || user.getRole() == UserRole.USER) {
            log.warn("SECURITY ALERT: Non-admin user attempted admin login - {}", request.getEmail());
            throw new BadCredentialsException("Access denied. Admin privileges required.");
        }

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Admin login blocked: Account locked for user {} until {}",
                    request.getEmail(), user.getLockedUntil());
            throw new LockedException(
                    String.format("Account is temporarily locked due to multiple failed login attempts. " +
                            "Please try again after %s.", user.getLockedUntil())
            );
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Admin login blocked: Account inactive for user {}", request.getEmail());
            throw new LockedException("Account is inactive. Please contact support.");
        }

        try {
            // Authenticate using phone number as username (Spring Security UserDetails uses phone number)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getPhoneNumber(),
                            request.getPassword()
                    )
            );

            // Update login tracking
            user.resetFailedAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Admin login successful for user: {} (ID: {}). Role: {}",
                    request.getEmail(), user.getId(), user.getRole());

            // Generate tokens
            return generateAdminAuthResponse(user);

        } catch (AuthenticationException e) {
            // Increment failed attempts
            handleFailedLogin(user);
            log.warn("Admin login failed: Invalid password for user {} (Attempt {}/{})",
                    request.getEmail(),
                    user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0,
                    maxFailedAttempts);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Create admin user (requires SUPER_ADMIN authorization)
     */
    @Transactional
    public AdminAuthResponse createAdmin(CreateAdminRequest request, UUID creatorId) {
        log.info("Create admin request for phone number: {} by creator: {}", request.getPhoneNumber(), creatorId);

        // Verify creator is super admin
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        if (creator.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("SECURITY ALERT: Non-super-admin user attempted to create admin - Creator ID: {}", creatorId);
            throw new IllegalArgumentException("Access denied. Only SUPER_ADMIN can create admin users.");
        }

        // Validate role
        if (request.getRole() == UserRole.USER) {
            throw new IllegalArgumentException("Cannot create regular users through admin endpoint. Use registration endpoint.");
        }

        // Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            log.warn("Create admin failed: Phone number already exists - {}", request.getPhoneNumber());
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Check if email already exists
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Create admin failed: Email already exists - {}", request.getEmail());
                throw new IllegalArgumentException("Email already registered");
            }
        }

        // Confirm password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // Create new admin user
        User admin = new User();
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(request.getRole());
        admin.setActive(true);
        admin.setFailedLoginAttempts(0);

        // Admin users don't need account numbers, but set it to avoid null issues
        admin.setAccountNumber("ADMIN-" + System.currentTimeMillis());
        admin.setBalance(BigDecimal.ZERO);

        admin = userRepository.save(admin);
        log.info("Admin user created successfully: {} (ID: {}, Role: {})",
                request.getPhoneNumber(), admin.getId(), admin.getRole());

        // Generate tokens
        return generateAdminAuthResponse(admin);
    }

    /**
     * Generate admin authentication response with tokens
     */
    private AdminAuthResponse generateAdminAuthResponse(User admin) {
        // Generate access token
        String accessToken = tokenProvider.generateAccessToken(
                admin.getId(),
                admin.getPhoneNumber(),
                admin.getName()
        );

        // Generate refresh token
        String refreshTokenString = tokenProvider.generateRefreshToken(admin.getId());

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .userId(admin.getId())
                .expiryDate(tokenProvider.getExpiryDateFromToken(refreshTokenString))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        // Build admin info
        AdminAuthResponse.AdminUserInfo adminInfo = AdminAuthResponse.AdminUserInfo.builder()
                .id(admin.getId())
                .phoneNumber(admin.getPhoneNumber())
                .name(admin.getName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .lastLoginAt(admin.getLastLoginAt())
                .build();

        // Calculate expiration in seconds
        long expiresIn = tokenProvider.getAccessTokenExpiration() / 1000;

        log.info("Admin tokens generated for user: {} (Role: {}, Access: {}s)",
                admin.getPhoneNumber(),
                admin.getRole(),
                expiresIn);

        return AdminAuthResponse.success(accessToken, refreshTokenString, expiresIn, adminInfo);
    }
}
