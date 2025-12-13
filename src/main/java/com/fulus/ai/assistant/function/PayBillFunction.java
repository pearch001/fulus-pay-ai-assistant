package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.BillPaymentResult;
import com.fulus.ai.assistant.dto.PayBillRequest;
import com.fulus.ai.assistant.enums.BillType;
import com.fulus.ai.assistant.service.BillPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Spring AI Function Tool for paying bills
 * ⚠️ WARNING: This function executes real financial transactions
 */
@Component("payBillFunction")
@Description("Pay bills for electricity, water, airtime, data, or cable TV. This function executes REAL financial transactions. " +
        "Use extreme caution and ALWAYS confirm payment details with the user before executing. " +
        "Returns payment confirmation with token and transaction reference. " +
        "Only use when the user explicitly requests to pay a bill.")
@RequiredArgsConstructor
@Slf4j
public class PayBillFunction implements Function<PayBillRequest, String> {

    private final BillPaymentService billPaymentService;

    private static final String CURRENCY_SYMBOL = "₦";

    /**
     * Execute bill payment
     * ⚠️ This function performs REAL financial transactions
     *
     * @param request Request containing userId, billType, amount, accountNumber
     * @return Confirmation message with payment details
     */
    @Override
    public String apply(PayBillRequest request) {
        log.warn("BILL PAYMENT REQUESTED: user={}, type={}, amount={}, account={}",
                request.getUserId(), request.getBillType(), request.getAmount(), request.getAccountNumber());

        try {
            // Validate inputs
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return "❌ ERROR: User ID is required to pay bills.";
            }

            if (request.getBillType() == null || request.getBillType().trim().isEmpty()) {
                return "❌ ERROR: Bill type is required. Valid types: ELECTRICITY, WATER, AIRTIME, DATA, CABLE_TV.";
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                return "❌ ERROR: Amount must be greater than zero.";
            }

            if (request.getAmount() > 1_000_000) {
                return "❌ ERROR: Amount exceeds maximum payment limit of ₦1,000,000. " +
                        "Please contact support for large payments.";
            }

            if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
                return "❌ ERROR: Account number or meter number is required.";
            }

            // Parse bill type
            BillType billType;
            try {
                billType = BillType.valueOf(request.getBillType().toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return String.format("❌ ERROR: Invalid bill type '%s'. Valid types are: " +
                        "ELECTRICITY, WATER, AIRTIME, DATA, CABLE_TV.", request.getBillType());
            }

            log.warn("EXECUTING BILL PAYMENT: user={}, type={}, amount={}, account={}",
                    request.getUserId(), billType, request.getAmount(), request.getAccountNumber());

            // Execute payment
            BillPaymentResult result = billPaymentService.payBill(
                    request.getUserId(),
                    billType,
                    request.getAmount(),
                    request.getAccountNumber()
            );

            // Format response
            if (result.isSuccess()) {
                return formatSuccessResponse(result);
            } else {
                return formatErrorResponse(result.getMessage());
            }

        } catch (Exception e) {
            log.error("ERROR executing bill payment", e);
            return "❌ PAYMENT FAILED: An unexpected error occurred. " +
                    "Your money is safe. Please try again or contact support.";
        }
    }

    /**
     * Format success response
     */
    private String formatSuccessResponse(BillPaymentResult result) {
        StringBuilder response = new StringBuilder();

        response.append("✅ BILL PAYMENT SUCCESSFUL!\n");
        response.append("═══════════════════════════════════════\n\n");

        response.append(String.format("Bill Type:      %s\n", result.getBillType().getDisplayName()));
        response.append(String.format("Provider:       %s\n", result.getProviderName()));
        response.append(String.format("Account:        %s\n", maskAccountNumber(result.getAccountNumber())));
        response.append(String.format("Amount:         %s%,.2f\n", CURRENCY_SYMBOL, result.getAmount()));

        response.append("\n");
        response.append("PAYMENT DETAILS\n");
        response.append("───────────────────────────────────────\n");
        response.append(String.format("Payment Token:  %s\n", result.getPaymentToken()));
        response.append(String.format("Transaction ID: %s\n", result.getTransactionId()));
        response.append(String.format("Reference:      %s\n", result.getTransactionReference()));
        response.append(String.format("New Balance:    %s%,.2f\n", CURRENCY_SYMBOL, result.getNewBalance()));
        response.append(String.format("Time:           %s\n", result.getTimestamp()));

        response.append("\n");
        response.append("⚡ Important: Please save your payment token for future reference.\n");
        response.append("   You may need it to verify the transaction with the service provider.\n\n");
        response.append("Thank you for using Fulus Pay! 💚");

        return response.toString();
    }

    /**
     * Format error response
     */
    private String formatErrorResponse(String errorMessage) {
        return String.format("❌ PAYMENT FAILED\n\n%s\n\n" +
                "Please check your account details and try again. " +
                "If the problem persists, contact support.", errorMessage);
    }

    /**
     * Mask account number for privacy (show only last 4 digits)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
