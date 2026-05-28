/*
 * Copyright 2025 QuireBind Contributors
 *
 * This file is part of QuireBind.
 *
 * QuireBind is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuireBind is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with QuireBind.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.maiitsoh.quirebind.core.pdf;

import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.ImposedSheet;
import com.maiitsoh.quirebind.core.model.MarkConfig;
import com.maiitsoh.quirebind.core.model.SewingConfig;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.Signature;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Writes composed {@link Signature} objects to an imposed output PDF.
 *
 * <h3>Output layout — FOLIO</h3>
 * Each {@link ImposedSheet} produces two consecutive PDF pages (front then back):
 * <ul>
 *   <li><b>Front</b>: landscape sheet, left half = {@code frontPages[0]},
 *       right half = {@code frontPages[1]}</li>
 *   <li><b>Back</b>: landscape sheet, left half = {@code backPages[0]},
 *       right half = {@code backPages[1]}</li>
 * </ul>
 *
 * <p>Intended for duplex printing with <em>short-edge flip</em> on landscape paper.
 * Source pages are scaled uniformly to fill each half while preserving aspect ratio.
 *
 * <p>Pages of type {@link PageType#CONTENT} with a {@code sourcePageIndex} set are
 * imported from the provided source PDF. All other page types produce a blank half.
 *
 * <p>{@link PaperSize#CUSTOM} is not supported in Phase 1.
 */
public final class PdfImpositionWriter {

    private PdfImpositionWriter() {
    }

    /**
     * Writes the imposed output PDF without marks or folios.
     *
     * @param signatures    composed signatures; must not be null
     * @param sourcePdfPath path to the source PDF, or {@code null} if all pages are blank
     * @param outputPath    destination path; must not be null
     * @param paperSize     book page size; must not be null; {@code CUSTOM} is unsupported
     * @throws IOException                   if any file operation fails
     * @throws UnsupportedOperationException if {@code paperSize} is {@link PaperSize#CUSTOM}
     */
    public static void write(
            List<Signature> signatures,
            Path sourcePdfPath,
            Path outputPath,
            PaperSize paperSize) throws IOException {
        write(signatures, sourcePdfPath, outputPath, paperSize,
                MarkConfig.builder().build(), null);
    }

    /**
     * Writes the imposed output PDF with optional marks and folios.
     *
     * @param signatures       composed signatures; must not be null
     * @param sourcePdfPath    path to the source PDF, or {@code null} if all pages are blank
     * @param outputPath       destination path; must not be null
     * @param paperSize        book page size; must not be null; {@code CUSTOM} is unsupported
     * @param markConfig       controls which output marks are rendered; null disables all marks
     * @param numberingConfig  controls folio rendering; null disables folio output
     * @throws IOException                   if any file operation fails
     * @throws UnsupportedOperationException if {@code paperSize} is {@link PaperSize#CUSTOM}
     */
    public static void write(
            List<Signature> signatures,
            Path sourcePdfPath,
            Path outputPath,
            PaperSize paperSize,
            MarkConfig markConfig,
            NumberingConfig numberingConfig) throws IOException {
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(paperSize, "paperSize");

        MarkConfig marks = markConfig != null ? markConfig : MarkConfig.builder().build();
        PDRectangle bookRect = bookPageRect(paperSize);
        PDRectangle sheetRect = new PDRectangle(bookRect.getWidth() * 2, bookRect.getHeight());
        int totalSigs = signatures.size();

        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDDocument srcDoc = sourcePdfPath != null ? Loader.loadPDF(sourcePdfPath.toFile()) : null;
        try {
            try (PDDocument outDoc = new PDDocument()) {
                LayerUtility layers = new LayerUtility(outDoc);
                for (Signature sig : signatures) {
                    int sheetsInSig = sig.getSheets().size();
                    for (ImposedSheet sheet : sig.getSheets()) {
                        addSheetPage(outDoc, layers, srcDoc, sheetRect, bookRect,
                                sheet.getFrontPages(), marks, numberingConfig, font,
                                sheet.getSheetIndex(), sheetsInSig,
                                sig.getSignatureIndex(), totalSigs);
                        addSheetPage(outDoc, layers, srcDoc, sheetRect, bookRect,
                                sheet.getBackPages(), marks, numberingConfig, font,
                                sheet.getSheetIndex(), sheetsInSig,
                                sig.getSignatureIndex(), totalSigs);
                    }
                }
                outDoc.save(outputPath.toFile());
            }
        } finally {
            if (srcDoc != null) {
                srcDoc.close();
            }
        }
    }

    private static void addSheetPage(
            PDDocument outDoc,
            LayerUtility layers,
            PDDocument srcDoc,
            PDRectangle sheetRect,
            PDRectangle bookRect,
            List<QuirePage> pages,
            MarkConfig marks,
            NumberingConfig numberingConfig,
            PDFont font,
            int sheetIndex,
            int sheetsInSig,
            int sigIndex,
            int totalSigs) throws IOException {
        PDPage outPage = new PDPage(sheetRect);
        outDoc.addPage(outPage);
        float halfW = bookRect.getWidth();
        float halfH = bookRect.getHeight();
        try (PDPageContentStream cs = new PDPageContentStream(outDoc, outPage)) {
            placePageOnSheet(outDoc, layers, cs, srcDoc, pages.get(0), 0f, halfW, halfH);
            placePageOnSheet(outDoc, layers, cs, srcDoc, pages.get(1), halfW, halfW, halfH);
            if (marks.isFoldLines()) {
                renderFoldLine(cs, halfW, halfH);
            }
            if (marks.isTrimLines()) {
                renderTrimLines(cs, 0f, halfW, halfH);
                renderTrimLines(cs, halfW, halfW, halfH);
            }
            if (marks.isSignatureProofMarkers()) {
                renderSignatureProofMark(cs, halfW, halfH, sigIndex, totalSigs);
            }
            if (marks.isSewingHoles()) {
                SewingConfig sc = marks.getSewingConfig().orElseGet(SewingConfig::defaults);
                renderSewingHoles(cs, halfW, halfH, sc);
            }
            if (numberingConfig != null) {
                renderFolio(cs, pages.get(0), numberingConfig, font, 0f, halfW, halfH);
                renderFolio(cs, pages.get(1), numberingConfig, font, halfW, halfW, halfH);
            }
        }
    }

    private static void placePageOnSheet(
            PDDocument outDoc,
            LayerUtility layers,
            PDPageContentStream cs,
            PDDocument srcDoc,
            QuirePage page,
            float xOffset,
            float halfW,
            float halfH) throws IOException {
        if (srcDoc == null) {
            return;
        }
        if (page.getPageType() != PageType.CONTENT) {
            return;
        }
        if (page.getSourcePageIndex().isEmpty()) {
            return;
        }
        int srcIdx = page.getSourcePageIndex().get();
        PDFormXObject form = layers.importPageAsForm(srcDoc, srcIdx);
        PDRectangle srcBox = srcDoc.getPage(srcIdx).getMediaBox();
        float scale = Math.min(halfW / srcBox.getWidth(), halfH / srcBox.getHeight());
        float tx = xOffset + (halfW - srcBox.getWidth() * scale) / 2f;
        float ty = (halfH - srcBox.getHeight() * scale) / 2f;
        cs.saveGraphicsState();
        cs.transform(new Matrix(scale, 0, 0, scale, tx, ty));
        cs.drawForm(form);
        cs.restoreGraphicsState();
    }

    private static void renderFoldLine(
            PDPageContentStream cs, float halfW, float halfH) throws IOException {
        cs.saveGraphicsState();
        cs.setStrokingColor(new Color(0.65f, 0.65f, 0.65f));
        cs.setLineWidth(0.5f);
        cs.moveTo(halfW, 0);
        cs.lineTo(halfW, halfH);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private static void renderTrimLines(
            PDPageContentStream cs, float xOffset, float halfW, float halfH) throws IOException {
        float trimLen = 8f;
        float inset = 4f;
        cs.saveGraphicsState();
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.25f);
        // Bottom-left corner
        cs.moveTo(xOffset + inset + trimLen, inset);
        cs.lineTo(xOffset + inset, inset);
        cs.lineTo(xOffset + inset, inset + trimLen);
        cs.stroke();
        // Bottom-right corner
        cs.moveTo(xOffset + halfW - inset - trimLen, inset);
        cs.lineTo(xOffset + halfW - inset, inset);
        cs.lineTo(xOffset + halfW - inset, inset + trimLen);
        cs.stroke();
        // Top-left corner
        cs.moveTo(xOffset + inset + trimLen, halfH - inset);
        cs.lineTo(xOffset + inset, halfH - inset);
        cs.lineTo(xOffset + inset, halfH - inset - trimLen);
        cs.stroke();
        // Top-right corner
        cs.moveTo(xOffset + halfW - inset - trimLen, halfH - inset);
        cs.lineTo(xOffset + halfW - inset, halfH - inset);
        cs.lineTo(xOffset + halfW - inset, halfH - inset - trimLen);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private static void renderSignatureProofMark(
            PDPageContentStream cs, float halfW, float halfH,
            int sigIndex, int totalSigs) throws IOException {
        float markH = 6f;
        float markW = 14f;
        float rangeH = halfH * 0.8f;
        float startY = halfH * 0.1f;
        float denominator = Math.max(totalSigs - 1, 1);
        float y = startY + (float) sigIndex / denominator * rangeH;
        float x = halfW - markW / 2f;
        float shade = 0.2f + 0.6f * ((float) sigIndex / Math.max(totalSigs, 1));
        cs.saveGraphicsState();
        cs.setNonStrokingColor(new Color(shade, shade * 0.5f, 0.1f));
        cs.addRect(x, y, markW, markH);
        cs.fill();
        cs.restoreGraphicsState();
    }

    private static void renderSewingHoles(
            PDPageContentStream cs, float halfW, float halfH,
            SewingConfig sewingConfig) throws IOException {
        int numHoles = sewingConfig.getHoleCount();
        float mmToPt = 72f / 25.4f;
        float endMarginPt = (float) (sewingConfig.getEndMarginMm() * mmToPt);
        float y0 = endMarginPt;
        float y1 = halfH - endMarginPt;
        float dotSize = 3f;
        cs.saveGraphicsState();
        cs.setNonStrokingColor(Color.BLACK);
        for (int i = 0; i < numHoles; i++) {
            float y = y0 + (float) i / (numHoles - 1) * (y1 - y0);
            float x = halfW - dotSize / 2f;
            cs.addRect(x, y, dotSize, dotSize);
            cs.fill();
        }
        cs.restoreGraphicsState();
    }

    private static void renderFolio(
            PDPageContentStream cs,
            QuirePage page,
            NumberingConfig numberingConfig,
            PDFont font,
            float xOffset,
            float halfW,
            float halfH) throws IOException {
        if (page.getLogicalPageNumber().isEmpty()) {
            return;
        }
        int num = page.getLogicalPageNumber().get();
        String text = String.valueOf(num);
        float fontSize = 8f;
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float margin = 14f;
        FolioPosition pos = numberingConfig.getFolioPosition();
        boolean isLeftHalf = xOffset < halfW;
        float x;
        float y;
        if (pos == FolioPosition.BOTTOM_OUTER) {
            y = margin;
            x = isLeftHalf ? margin : xOffset + halfW - margin - textWidth;
        } else if (pos == FolioPosition.BOTTOM_CENTER) {
            y = margin;
            x = xOffset + halfW / 2f - textWidth / 2f;
        } else if (pos == FolioPosition.BOTTOM_INNER) {
            y = margin;
            x = isLeftHalf ? xOffset + halfW - margin - textWidth : xOffset + margin;
        } else if (pos == FolioPosition.TOP_OUTER) {
            y = halfH - margin - fontSize;
            x = isLeftHalf ? margin : xOffset + halfW - margin - textWidth;
        } else if (pos == FolioPosition.TOP_CENTER) {
            y = halfH - margin - fontSize;
            x = xOffset + halfW / 2f - textWidth / 2f;
        } else {
            // TOP_INNER (default fallback)
            y = halfH - margin - fontSize;
            x = isLeftHalf ? xOffset + halfW - margin - textWidth : xOffset + margin;
        }
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static PDRectangle bookPageRect(PaperSize size) {
        return switch (size) {
            case A4 -> PDRectangle.A4;
            case A5 -> PDRectangle.A5;
            case A3 -> PDRectangle.A3;
            case LETTER -> PDRectangle.LETTER;
            case LEGAL -> PDRectangle.LEGAL;
            case HALF_LETTER -> new PDRectangle(396f, 612f);
            case CUSTOM -> throw new UnsupportedOperationException(
                    "CUSTOM paper size is not supported in Phase 1");
        };
    }
}
