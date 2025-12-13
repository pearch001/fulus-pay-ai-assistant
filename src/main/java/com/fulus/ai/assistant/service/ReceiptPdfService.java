package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.ReceiptDTO;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating PDF receipts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptPdfService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // Colors - Primary brand color #D4843B with complementary colors
    private static final BaseColor PRIMARY_GOLD = new BaseColor(212, 132, 59); // #D4843B
    private static final BaseColor SUCCESS_GREEN = new BaseColor(76, 175, 80); // #4CAF50
    private static final BaseColor ERROR_RED = new BaseColor(244, 67, 54); // #F44336
    private static final BaseColor WARNING_AMBER = new BaseColor(255, 193, 7); // #FFC107
    private static final BaseColor LIGHT_GREY = new BaseColor(245, 245, 245); // #F5F5F5
    private static final BaseColor DARK_GREY = new BaseColor(66, 66, 66); // #424242
    private static final BaseColor ACCENT_BLUE = new BaseColor(33, 150, 243); // #2196F3

    /**
     * Generate receipt PDF from transaction ID
     */
    public byte[] generateReceipt(UUID transactionId) throws Exception {
        log.info("Generating receipt for transaction: {}", transactionId);

        // Fetch transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // Fetch user
        User user = userRepository.findById(transaction.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Build receipt DTO
        ReceiptDTO receipt = buildReceiptDTO(transaction, user);

        // Generate PDF
        return generatePdf(receipt);
    }

    /**
     * Build receipt DTO from transaction
     */
    private ReceiptDTO buildReceiptDTO(Transaction transaction, User user) {
        ReceiptDTO.ReceiptStatus status = ReceiptDTO.ReceiptStatus.fromTransactionStatus(transaction.getStatus());

        String title = switch (status) {
            case SUCCESS -> "Transaction Successful";
            case FAILED -> "Transaction Failed";
            case PENDING -> "Transaction Pending";
        };

        String subtitle = switch (status) {
            case SUCCESS -> "Your transaction has been approved";
            case FAILED -> "Your transaction could not be completed";
            case PENDING -> "Your transaction is being processed";
        };

        String amountLabel = switch (transaction.getType()) {
            case DEBIT -> "Amount Sent";
            case CREDIT -> "Amount Received";
        };

        return ReceiptDTO.builder()
                .title(title)
                .subtitle(subtitle)
                .status(status)
                .amount(transaction.getAmount())
                //.amountLabel(amountLabel)
                .senderName(user.getName())
                .senderAccount(user.getAccountNumber())
                .recipientName(transaction.getRecipientPhoneNumber() != null ?
                        getRecipientName(transaction.getRecipientPhoneNumber()) : "N/A")
                .recipientAccount(transaction.getRecipientPhoneNumber())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .timestamp(transaction.getCreatedAt())
                .transactionType(transaction.getType().name())
                .balanceAfter(transaction.getBalanceAfter())
                .build();
    }

    /**
     * Get recipient name from phone number
     */
    private String getRecipientName(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(User::getName)
                .orElse("External Account");
    }

    /**
     * Generate PDF document
     */
    private byte[] generatePdf(ReceiptDTO receipt) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Add spacing from top
        document.add(new Paragraph("\n\n\n"));

        // Header Section
        addHeader(document, receipt);

        // Status Icon Section
        addStatusIcon(document, receipt);

        // Amount Section
        addAmountSection(document, receipt);

        // Divider
        addDivider(document);

        // Transaction Details
        addTransactionDetails(document, receipt);

        // Footer
        addFooter(document);

        document.close();

        log.info("Receipt PDF generated successfully for reference: {}", receipt.getReference());
        return baos.toByteArray();
    }

    /**
     * Add header section with branding
     */
    private void addHeader(Document document, ReceiptDTO receipt) throws DocumentException {
        // Brand name
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Font.BOLD, PRIMARY_GOLD);
        Paragraph brand = new Paragraph("SyncPay", brandFont);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(10);
        document.add(brand);

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.BOLD, DARK_GREY);
        Paragraph title = new Paragraph(receipt.getTitle(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Subtitle
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, DARK_GREY);
        Paragraph subtitle = new Paragraph(receipt.getSubtitle(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
    }

    /**
     * Add logo section with brand logo image
     */
    private void addStatusIcon(Document document, ReceiptDTO receipt) throws DocumentException {
        try {
            // Load logo image from resources
            String logoPath = "/Container.png";
            Image logo = Image.getInstance(getClass().getResource(logoPath));

            // Scale the logo to appropriate size (60x60 pixels as in the original SVG)
            logo.scaleAbsolute(60, 60);

            // Center the logo
            logo.setAlignment(Element.ALIGN_CENTER);
            logo.setSpacingBefore(10);
            logo.setSpacingAfter(20);

            document.add(logo);

        } catch (Exception e) {
            log.warn("Failed to load logo image, using fallback text: {}", e.getMessage());

            // Fallback to text-based logo if image fails to load
            Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.BOLD, PRIMARY_GOLD);
            Paragraph logoText = new Paragraph("SP", logoFont);
            logoText.setAlignment(Element.ALIGN_CENTER);
            logoText.setSpacingBefore(10);
            logoText.setSpacingAfter(20);
            document.add(logoText);
        }
    }

    /**
     * Add amount section
     */
    private void addAmountSection(Document document, ReceiptDTO receipt) throws DocumentException {
        // Amount Label
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, DARK_GREY);
        Paragraph amountLabel = new Paragraph(receipt.getAmountLabel(), labelFont);
        amountLabel.setAlignment(Element.ALIGN_CENTER);
        amountLabel.setSpacingAfter(5);
        document.add(amountLabel);

        // Amount - use primary gold color
        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, Font.BOLD, PRIMARY_GOLD);
        Paragraph amount = new Paragraph("₦" + String.format("%,.2f", receipt.getAmount()), amountFont);
        amount.setAlignment(Element.ALIGN_CENTER);
        amount.setSpacingAfter(20);
        document.add(amount);
    }

    /**
     * Add divider line
     */
    private void addDivider(Document document) throws DocumentException {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(80);
        divider.setHorizontalAlignment(Element.ALIGN_CENTER);
        divider.setSpacingBefore(10);
        divider.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(LIGHT_GREY);
        cell.setBorderWidthBottom(1);
        cell.setPadding(0);
        divider.addCell(cell);

        document.add(divider);
    }

    /**
     * Add transaction details section
     */
    private void addTransactionDetails(Document document, ReceiptDTO receipt) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(85);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingBefore(15);
        table.setSpacingAfter(20);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, DARK_GREY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, DARK_GREY);
        Font accentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, PRIMARY_GOLD);

        // Recipient row
        addDetailRow(table, "Recipient", receipt.getRecipientName(), labelFont, valueFont);

        // Reference row - use accent color
        addDetailRow(table, "Reference", receipt.getReference(), labelFont, accentFont);

        // Sender row
        addDetailRow(table, "Sender", receipt.getSenderName(), labelFont, valueFont);

        // Date row
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        addDetailRow(table, "Date", receipt.getTimestamp().format(formatter), labelFont, valueFont);

        // Type row
        addDetailRow(table, "Type", receipt.getTransactionType(), labelFont, valueFont);

        // Balance row - use accent color
        //addDetailRow(table, "Balance After", "₦" + String.format("%,.2f", receipt.getBalanceAfter()), labelFont, accentFont);

        document.add(table);
    }

    /**
     * Add detail row to table
     */
    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        // Label cell (left)
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        // Value cell (right)
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    /**
     * Add footer with branding
     */
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph("\n\n"));

        // Divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(60);
        divider.setHorizontalAlignment(Element.ALIGN_CENTER);
        divider.setSpacingBefore(10);
        divider.setSpacingAfter(15);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(PRIMARY_GOLD);
        cell.setBorderWidthTop(2);
        cell.setPadding(0);
        divider.addCell(cell);
        document.add(divider);

        // Footer text with brand color
        Font brandFooterFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, PRIMARY_GOLD);
        Paragraph footer = new Paragraph("FULUS PAY", brandFooterFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingAfter(5);
        document.add(footer);

        Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, DARK_GREY);
        Paragraph disclaimer = new Paragraph("This is a computer-generated receipt and requires no signature", disclaimerFont);
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        document.add(disclaimer);
    }
}
