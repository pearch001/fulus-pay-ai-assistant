package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.SavingsCalculationResult;
import com.fulus.ai.assistant.dto.SavingsGoalResponse;
import com.fulus.ai.assistant.entity.SavingsAccount;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.exception.InvalidAmountException;
import com.fulus.ai.assistant.exception.UserNotFoundException;
import com.fulus.ai.assistant.repository.SavingsAccountRepository;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsService {

    private final UserRepository userRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;

    private static final double DEFAULT_ANNUAL_INTEREST_RATE = 5.0; // 5% annual interest rate

    /**
     * Create a savings goal for a user
     *
     * @param userId              UUID of the user
     * @param targetAmount        Target savings amount
     * @param monthlyContribution Monthly contribution amount
     * @param durationMonths      Duration in months
     * @return SavingsGoalResponse with projection details
     */
    @Transactional
    public SavingsGoalResponse createSavingsGoal(
            String userId,
            double targetAmount,
            double monthlyContribution,
            int durationMonths) {

        return createSavingsGoal(userId, targetAmount, monthlyContribution, durationMonths, DEFAULT_ANNUAL_INTEREST_RATE);
    }

    /**
     * Create a savings goal with custom interest rate
     */
    @Transactional
    public SavingsGoalResponse createSavingsGoal(
            String userId,
            double targetAmount,
            double monthlyContribution,
            int durationMonths,
            double annualInterestRate) {

        log.info("Creating savings goal: userId={}, target={}, monthly={}, duration={} months",
                userId, targetAmount, monthlyContribution, durationMonths);

        try {
            // Validate inputs
            validateAmount(targetAmount, "Target amount");
            validateAmount(monthlyContribution, "Monthly contribution");
            if (durationMonths < 1 || durationMonths > 600) {
                throw new IllegalArgumentException("Duration must be between 1 and 600 months");
            }

            // Get user
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Calculate projected returns
            SavingsCalculationResult calculation = calculateReturns(
                    0.0, // Starting with 0 principal
                    monthlyContribution,
                    annualInterestRate,
                    durationMonths
            );

            // Create savings account
            SavingsAccount savingsAccount = new SavingsAccount();
            savingsAccount.setUserId(user.getId());
            savingsAccount.setTargetAmount(BigDecimal.valueOf(targetAmount));
            savingsAccount.setCurrentAmount(BigDecimal.ZERO);
            savingsAccount.setMonthlyContribution(BigDecimal.valueOf(monthlyContribution));
            savingsAccount.setInterestRate(BigDecimal.valueOf(annualInterestRate));
            savingsAccount.setStartDate(LocalDate.now());
            savingsAccount.setMaturityDate(LocalDate.now().plusMonths(durationMonths));

            savingsAccount = savingsAccountRepository.save(savingsAccount);

            log.info("Savings goal created successfully: accountId={}", savingsAccount.getId());

            return SavingsGoalResponse.success(
                    savingsAccount.getId(),
                    savingsAccount.getTargetAmount(),
                    savingsAccount.getMonthlyContribution(),
                    calculation.getFinalBalance(),
                    calculation.getTotalInterestEarned(),
                    annualInterestRate,
                    savingsAccount.getStartDate(),
                    savingsAccount.getMaturityDate(),
                    durationMonths,
                    calculation.getExplanation()
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            return SavingsGoalResponse.failure(e.getMessage());
        } catch (UserNotFoundException | InvalidAmountException e) {
            log.error("Savings goal creation failed: {}", e.getMessage());
            return SavingsGoalResponse.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating savings goal", e);
            return SavingsGoalResponse.failure("Failed to create savings goal due to unexpected error");
        }
    }

    /**
     * Calculate returns using compound interest formula with monthly contributions
     *
     * Formula breakdown:
     * - Monthly interest rate = Annual rate / 12
     * - For principal: FV = PV * (1 + r)^n
     * - For contributions: FV = PMT * [(1 + r)^n - 1] / r
     *
     * @param principal            Initial principal amount
     * @param monthlyContribution  Monthly contribution amount
     * @param annualInterestRate   Annual interest rate (as percentage, e.g., 5.0 for 5%)
     * @param months               Number of months
     * @return SavingsCalculationResult with detailed breakdown
     */
    public SavingsCalculationResult calculateReturns(
            double principal,
            double monthlyContribution,
            double annualInterestRate,
            int months) {

        log.debug("Calculating returns: principal={}, monthly={}, rate={}%, months={}",
                principal, monthlyContribution, annualInterestRate, months);

        // Convert annual rate to monthly rate (as decimal)
        double monthlyRate = (annualInterestRate / 100.0) / 12.0;

        BigDecimal principalBD = BigDecimal.valueOf(principal);
        BigDecimal monthlyContributionBD = BigDecimal.valueOf(monthlyContribution);

        // Calculate future value of principal with compound interest
        // FV_principal = PV * (1 + r)^n
        double principalFutureValue = principal * Math.pow(1 + monthlyRate, months);

        // Calculate future value of monthly contributions (annuity)
        // FV_annuity = PMT * [(1 + r)^n - 1] / r
        double contributionsFutureValue;
        if (monthlyRate == 0) {
            // If interest rate is 0, simply multiply contributions by months
            contributionsFutureValue = monthlyContribution * months;
        } else {
            contributionsFutureValue = monthlyContribution * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate);
        }

        // Total future value
        BigDecimal finalBalance = BigDecimal.valueOf(principalFutureValue + contributionsFutureValue)
                .setScale(2, RoundingMode.HALF_UP);

        // Total contributions made
        BigDecimal totalContributions = principalBD.add(monthlyContributionBD.multiply(BigDecimal.valueOf(months)))
                .setScale(2, RoundingMode.HALF_UP);

        // Total interest earned
        BigDecimal totalInterestEarned = finalBalance.subtract(totalContributions)
                .setScale(2, RoundingMode.HALF_UP);

        // Generate explanation
        String explanation = SavingsCalculationResult.generateExplanation(
                principalBD,
                monthlyContributionBD,
                totalContributions,
                totalInterestEarned,
                finalBalance,
                annualInterestRate,
                months
        );

        return SavingsCalculationResult.builder()
                .principal(principalBD)
                .monthlyContribution(monthlyContributionBD)
                .totalContributions(totalContributions)
                .totalInterestEarned(totalInterestEarned)
                .finalBalance(finalBalance)
                .annualInterestRate(annualInterestRate)
                .months(months)
                .explanation(explanation)
                .build();
    }

    /**
     * Get savings balance for a user
     *
     * @param userId UUID of the user
     * @return SavingsAccount or null if not found
     */
    @Transactional(readOnly = true)
    public SavingsAccount getSavingsBalance(String userId) {
        log.debug("Fetching savings balance for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);

            // Verify user exists
            userRepository.findById(userUuid)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Get active savings accounts for user
            List<SavingsAccount> accounts = savingsAccountRepository.findActiveAccountsByUser(userUuid);

            if (accounts.isEmpty()) {
                log.warn("No active savings account found for user: {}", userId);
                return null;
            }

            // Return the first active account (or you could return all)
            SavingsAccount account = accounts.get(0);
            log.debug("Savings balance: current={}, target={}",
                    account.getCurrentAmount(), account.getTargetAmount());

            return account;

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new UserNotFoundException("Invalid user ID format: " + userId);
        }
    }

    /**
     * Deposit money into savings account
     *
     * @param userId UUID of the user
     * @param amount Amount to deposit
     * @return Transaction record
     */
    @Transactional
    public Transaction depositToSavings(String userId, double amount) {
        log.info("Depositing to savings: userId={}, amount={}", userId, amount);

        // Validate amount
        validateAmount(amount, "Deposit amount");

        BigDecimal depositAmount = BigDecimal.valueOf(amount);

        // Get user
        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Get or create savings account
        List<SavingsAccount> accounts = savingsAccountRepository.findActiveAccountsByUser(userUuid);
        SavingsAccount savingsAccount;

        if (accounts.isEmpty()) {
            log.warn("No active savings account found. Please create a savings goal first.");
            throw new IllegalArgumentException("No active savings account found. Please create a savings goal first.");
        }

        savingsAccount = accounts.get(0);

        // Update savings account balance
        savingsAccount.setCurrentAmount(savingsAccount.getCurrentAmount().add(depositAmount));
        savingsAccountRepository.save(savingsAccount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setType(TransactionType.DEBIT);
        transaction.setCategory(TransactionCategory.SAVINGS);
        transaction.setAmount(depositAmount);
        transaction.setDescription(String.format("Deposit to savings goal (Current: ₦%.2f / Target: ₦%.2f)",
                savingsAccount.getCurrentAmount(), savingsAccount.getTargetAmount()));
        transaction.setBalanceAfter(user.getBalance());
        transaction.setReference(generateTransactionReference());
        transaction.setStatus(TransactionStatus.COMPLETED);

        transaction = transactionRepository.save(transaction);

        log.info("Savings deposit completed: reference={}, new balance={}",
                transaction.getReference(), savingsAccount.getCurrentAmount());

        return transaction;
    }

    /**
     * Validate amount is positive
     */
    private void validateAmount(double amount, String fieldName) {
        if (amount <= 0) {
            throw new InvalidAmountException(fieldName + " must be greater than 0");
        }
    }

    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        return "SAV-" + UUID.randomUUID().toString().toUpperCase().substring(0, 18);
    }
}
