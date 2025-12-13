package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionType;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for exporting transactions to CSV and PDF formats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionExportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate CSV export of transactions
     */
    public byte[] generateCSV(UUID userId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Generating CSV export for user: {} from {} to {}", userId, fromDate, toDate);

        List<Transaction> transactions = transactionRepository.findTransactionsForExport(userId, fromDate, toDate);

        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("Date,Reference,Type,Category,Description,Amount,Balance After,Status\n");

        // CSV Rows
        for (Transaction transaction : transactions) {
            csv.append(escapeCSV(transaction.getCreatedAt().format(DATE_FORMATTER))).append(",");
            csv.append(escapeCSV(transaction.getReference())).append(",");
            csv.append(escapeCSV(transaction.getType().toString())).append(",");
            csv.append(escapeCSV(transaction.getCategory().toString())).append(",");
            csv.append(escapeCSV(transaction.getDescription() != null ? transaction.getDescription() : "")).append(",");
            csv.append(formatCurrency(transaction.getAmount())).append(",");
            csv.append(formatCurrency(transaction.getBalanceAfter())).append(",");
            csv.append(escapeCSV(transaction.getStatus().toString())).append("\n");
        }

        log.info("CSV export generated with {} transactions", transactions.size());
        return csv.toString().getBytes();
    }

    /**
     * Generate PDF export of transactions
     */
    public byte[] generatePDF(UUID userId, LocalDateTime fromDate, LocalDateTime toDate) throws IOException, DocumentException {
        log.info("Generating PDF export for user: {} from {} to {}", userId, fromDate, toDate);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> transactions = transactionRepository.findTransactionsForExport(userId, fromDate, toDate);

        // Calculate summary
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal openingBalance = user.getBalance();

        if (!transactions.isEmpty()) {
            openingBalance = transactions.get(0).getBalanceAfter().subtract(
                    transactions.get(0).getType() == TransactionType.CREDIT
                    ? transactions.get(0).getAmount()
                    : transactions.get(0).getAmount().negate()
            );
        }

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.CREDIT) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else {
                totalExpenses = totalExpenses.add(transaction.getAmount());
            }
        }

        BigDecimal closingBalance = !transactions.isEmpty()
                ? transactions.get(transactions.size() - 1).getBalanceAfter()
                : user.getBalance();

        // Create PDF document
        Document document = new Document(PageSize.A4, 36, 36, 54, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 8);

            Paragraph title = new Paragraph("FULUS PAY - ACCOUNT STATEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // User Info
            document.add(new Paragraph("Account Holder: " + user.getFullName(), normalFont));
            document.add(new Paragraph("Phone: " + user.getPhoneNumber(), normalFont));
            document.add(new Paragraph("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"), normalFont));
            document.add(new Paragraph("Statement Period: " +
                    (fromDate != null ? fromDate.format(DATE_FORMATTER) : "Beginning") +
                    " to " +
                    (toDate != null ? toDate.format(DATE_FORMATTER) : "Present"), normalFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER), normalFont));
            document.add(new Paragraph(" ")); // Spacing

            // Summary Table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10);
            summaryTable.setSpacingAfter(20);

            addSummaryRow(summaryTable, "Opening Balance", formatCurrency(openingBalance), headerFont, normalFont);
            addSummaryRow(summaryTable, "Total Income", formatCurrency(totalIncome), normalFont, normalFont);
            addSummaryRow(summaryTable, "Total Expenses", formatCurrency(totalExpenses), normalFont, normalFont);
            addSummaryRow(summaryTable, "Closing Balance", formatCurrency(closingBalance), headerFont, normalFont);
            addSummaryRow(summaryTable, "Total Transactions", String.valueOf(transactions.size()), normalFont, normalFont);

            document.add(summaryTable);

            // Transactions Table
            if (!transactions.isEmpty()) {
                Paragraph transactionsTitle = new Paragraph("TRANSACTION DETAILS", headerFont);
                transactionsTitle.setSpacingBefore(10);
                transactionsTitle.setSpacingAfter(10);
                document.add(transactionsTitle);

                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2, 1.5f, 1.5f, 3, 1.5f, 1.5f});

                // Table Headers
                addTableHeader(table, "Date", smallFont);
                addTableHeader(table, "Type", smallFont);
                addTableHeader(table, "Category", smallFont);
                addTableHeader(table, "Description", smallFont);
                addTableHeader(table, "Amount", smallFont);
                addTableHeader(table, "Balance", smallFont);

                // Table Rows
                for (Transaction transaction : transactions) {
                    addTableCell(table, transaction.getCreatedAt().format(DATE_FORMATTER), smallFont);
                    addTableCell(table, transaction.getType().toString(), smallFont);
                    addTableCell(table, transaction.getCategory().toString(), smallFont);
                    addTableCell(table, transaction.getDescription() != null ? transaction.getDescription() : "-", smallFont);

                    // Amount with color
                    PdfPCell amountCell = new PdfPCell(new Phrase(formatCurrency(transaction.getAmount()), smallFont));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    amountCell.setPadding(5);
                    if (transaction.getType() == TransactionType.CREDIT) {
                        amountCell.setBackgroundColor(new BaseColor(200, 255, 200)); // Light green
                    } else {
                        amountCell.setBackgroundColor(new BaseColor(255, 200, 200)); // Light red
                    }
                    table.addCell(amountCell);

                    addTableCell(table, formatCurrency(transaction.getBalanceAfter()), smallFont, Element.ALIGN_RIGHT);
                }

                document.add(table);
            }

            // Footer
            Paragraph footer = new Paragraph("\n\nThis is a computer-generated statement and does not require a signature.",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            log.info("PDF export generated with {} transactions", transactions.size());

        } catch (DocumentException e) {
            log.error("Error generating PDF", e);
            throw e;
        }

        return out.toByteArray();
    }

    /**
     * Get filename for export
     */
    public String getExportFilename(String format, UUID userId, LocalDateTime fromDate, LocalDateTime toDate) {
        String dateRange = (fromDate != null ? fromDate.format(FILE_DATE_FORMATTER) : "all") +
                          "_to_" +
                          (toDate != null ? toDate.format(FILE_DATE_FORMATTER) : LocalDateTime.now().format(FILE_DATE_FORMATTER));

        return String.format("fulus_statement_%s_%s.%s",
                userId.toString().substring(0, 8),
                dateRange,
                format.toLowerCase());
    }

    // Helper methods

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatCurrency(BigDecimal amount) {
        return "₦" + String.format("%,.2f", amount);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }
}
