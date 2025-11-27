package com.munitax.pdf.service;

import com.munitax.pdf.model.TaxCalculationResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PdfGeneratorService {

    private static final float MARGIN = 50;
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    public byte[] generateTaxReturnPdf(TaxCalculationResult result) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                // Header
                y = drawHeader(contentStream, result, y);
                y -= 20;

                // Taxpayer Info Box
                y = drawTaxpayerInfo(contentStream, result, y);
                y -= 30;

                // Section A: Income
                y = drawSectionHeader(contentStream, "SECTION A: TAXABLE INCOME", y);
                y = drawLine(contentStream, "1. Total Qualifying Wages (W-2)", result.getW2TaxableIncome(), y, false);
                y = drawLine(contentStream, "2. Other Income (Schedule X)", result.getScheduleX().getTotalNetProfit(), y, false);
                y = drawHorizontalLine(contentStream, y - 5);
                y -= 10;
                y = drawLine(contentStream, "3. TOTAL TAXABLE INCOME", result.getTotalTaxableIncome(), y, true);
                y -= 20;

                // Section B: Tax & Credits
                y = drawSectionHeader(contentStream, "SECTION B: TAX CALCULATION", y);
                y = drawLine(contentStream, "4. Dublin Tax Due (2.0% of Line 3)", result.getMunicipalLiability(), y, false);
                y = drawLine(contentStream, "5. Credits for Tax Paid to Other Cities (Sch Y)", result.getScheduleY().getTotalCredit(), y, false);
                y = drawLine(contentStream, "6. Dublin Tax Withheld (W-2)", result.getTotalLocalWithheld(), y, false);
                
                double totalPayments = result.getScheduleY().getTotalCredit() + result.getTotalLocalWithheld();
                y = drawHorizontalLine(contentStream, y - 5);
                y -= 10;
                y = drawLine(contentStream, "7. TOTAL PAYMENTS & CREDITS", totalPayments, y, true);
                y -= 20;

                // Section C: Balance
                y = drawSectionHeader(contentStream, "SECTION C: BALANCE DUE / REFUND", y);
                double balance = result.getMunicipalBalance();
                if (balance < 0) {
                    y = drawLine(contentStream, "8. TAX DUE (Pay this amount)", Math.abs(balance), y, true);
                    y -= 10;
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                    contentStream.newLineAtOffset(MARGIN, y);
                    contentStream.showText("Make checks payable to: CITY OF DUBLIN");
                    contentStream.endText();
                } else {
                    y = drawLine(contentStream, "8. OVERPAYMENT (Refund/Credit)", balance, y, true);
                }

                // Footer / Signature
                y = 100;
                drawSignatureSection(contentStream, y);
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    private float drawHeader(PDPageContentStream contentStream, TaxCalculationResult result, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
        contentStream.newLineAtOffset(150, y);
        contentStream.showText("CITY OF DUBLIN");
        contentStream.endText();
        y -= 20;

        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        contentStream.newLineAtOffset(140, y);
        contentStream.showText("INCOME TAX RETURN");
        contentStream.endText();
        y -= 20;

        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contentStream.newLineAtOffset(200, y);
        contentStream.showText("Tax Year: " + result.getSettings().getTaxYear());
        contentStream.endText();

        if (result.getSettings().isAmendment()) {
            contentStream.setNonStrokingColor(200, 0, 0);
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            contentStream.newLineAtOffset(400, y);
            contentStream.showText("AMENDED RETURN");
            contentStream.endText();
            contentStream.setNonStrokingColor(0, 0, 0);
        }

        return y;
    }

    private float drawTaxpayerInfo(PDPageContentStream contentStream, TaxCalculationResult result, float y) throws IOException {
        float boxHeight = 60;
        float boxWidth = 500;

        // Draw box
        contentStream.setLineWidth(1);
        contentStream.addRect(MARGIN, y - boxHeight, boxWidth, boxHeight);
        contentStream.stroke();

        // Name and SSN
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        contentStream.newLineAtOffset(MARGIN + 10, y - 20);
        contentStream.showText("Name: " + result.getProfile().getName());
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 300, y - 20);
        String ssn = result.getProfile().getSsn() != null ? result.getProfile().getSsn() : "XXX-XX-XXXX";
        contentStream.showText("SSN: " + ssn);
        contentStream.endText();

        // Address
        if (result.getProfile().getAddress() != null) {
            var addr = result.getProfile().getAddress();
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 10, y - 35);
            contentStream.showText("Address: " + (addr.getStreet() != null ? addr.getStreet() : ""));
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 10, y - 50);
            String cityStateZip = String.format("%s, %s %s",
                    addr.getCity() != null ? addr.getCity() : "",
                    addr.getState() != null ? addr.getState() : "",
                    addr.getZip() != null ? addr.getZip() : "");
            contentStream.showText(cityStateZip);
            contentStream.endText();
        }

        return y - boxHeight;
    }

    private float drawSectionHeader(PDPageContentStream contentStream, String title, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(title);
        contentStream.endText();
        return y - 15;
    }

    private float drawLine(PDPageContentStream contentStream, String label, double value, float y, boolean bold) throws IOException {
        PDType1Font font = bold ?
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD) :
                new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        contentStream.beginText();
        contentStream.setFont(font, 10);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(label);
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(font, 10);
        String formattedValue = CURRENCY_FORMAT.format(value);
        contentStream.newLineAtOffset(500, y);
        contentStream.showText(formattedValue);
        contentStream.endText();

        return y - 15;
    }

    private float drawHorizontalLine(PDPageContentStream contentStream, float y) throws IOException {
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(550, y);
        contentStream.stroke();
        return y;
    }

    private void drawSignatureSection(PDPageContentStream contentStream, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText("I certify that I have examined this return and to the best of my knowledge and belief it is true, correct and complete.");
        contentStream.endText();

        y -= 20;
        // Signature line
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(250, y);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        contentStream.newLineAtOffset(MARGIN, y - 10);
        contentStream.showText("Taxpayer Signature");
        contentStream.endText();

        // Date line
        contentStream.moveTo(300, y);
        contentStream.lineTo(400, y);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.newLineAtOffset(300, y - 10);
        contentStream.showText("Date");
        contentStream.endText();
    }
}
