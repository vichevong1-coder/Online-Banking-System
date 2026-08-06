package com.obs.backend.feature.statement.service.impl;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.user.entity.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

// US-019: renders the statement as a plain, fixed-width-column PDF — no table library needed
// for what's still a single-column ledger.
@Component
public class PdfStatementRenderer {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 16f;
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] render(
            User user, Account account, List<Transaction> transactions, LocalDate fromDate, LocalDate toDate) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = newPage(document);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = PAGE_HEIGHT - MARGIN;

            y = writeLine(content, y, FONT_BOLD, 16, "Account Statement");
            y -= LINE_HEIGHT / 2;
            y = writeLine(content, y, FONT_REGULAR, 11,
                    "Account holder: %s %s".formatted(user.getFirstName(), user.getLastName()));
            y = writeLine(content, y, FONT_REGULAR, 11, "Account number: %s".formatted(account.getAccountNumber()));
            y = writeLine(content, y, FONT_REGULAR, 11, "Currency: %s".formatted(account.getCurrency()));
            y = writeLine(content, y, FONT_REGULAR, 11,
                    "Period: %s to %s".formatted(fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)));
            y -= LINE_HEIGHT;

            y = writeLine(content, y, FONT_BOLD, 10, formatRow("Date", "Type", "Description", "Amount", "Balance"));

            if (transactions.isEmpty()) {
                y = writeLine(content, y, FONT_REGULAR, 10, "No transactions in this period.");
            }

            for (Transaction transaction : transactions) {
                if (y < MARGIN + LINE_HEIGHT) {
                    content.close();
                    page = newPage(document);
                    content = new PDPageContentStream(document, page);
                    y = PAGE_HEIGHT - MARGIN;
                }
                String description = transaction.getDescription() == null ? "" : transaction.getDescription();
                y = writeLine(content, y, FONT_REGULAR, 10, formatRow(
                        transaction.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMAT),
                        transaction.getType().toString(),
                        description,
                        transaction.getAmount().toPlainString(),
                        transaction.getBalanceAfter().toPlainString()));
            }

            content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PDPage newPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        return page;
    }

    private float writeLine(PDPageContentStream content, float y, PDFont font, float fontSize, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(MARGIN, y);
        content.showText(sanitize(text));
        content.endText();
        return y - LINE_HEIGHT;
    }

    private String formatRow(String date, String type, String description, String amount, String balance) {
        return "%-12s %-14s %-30.30s %14s %14s".formatted(date, type, description, amount, balance);
    }

    // Helvetica's default WinAnsi encoding throws on glyphs it doesn't have; better to fall back
    // to "?" than 500 the whole statement over one unsupported character.
    private String sanitize(String text) {
        return text.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
