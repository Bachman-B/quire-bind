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

import com.maiitsoh.quirebind.core.creep.CreepTransformApplier;
import com.maiitsoh.quirebind.core.model.CreepConfig;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * Delegates to {@link #write(List, Map, Path, PaperSize, MarkConfig, NumberingConfig, CreepConfig)}.
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
        Map<String, Path> docs = sourcePdfPath != null
                ? Map.of(sourcePdfPath.toString(), sourcePdfPath)
                : Map.of();
        write(signatures, docs, outputPath, paperSize, markConfig, numberingConfig, null);
    }

    /**
     * Writes the imposed output PDF sourcing pages from multiple source documents,
     * without creep compensation.
     *
     * @param signatures       composed signatures; must not be null
     * @param sourceDocPaths   map from document ID to file path; may be null or empty
     * @param outputPath       destination path; must not be null
     * @param paperSize        book page size; must not be null; {@code CUSTOM} is unsupported
     * @param markConfig       controls which output marks are rendered; null disables all marks
     * @param numberingConfig  controls folio rendering; null disables folio output
     * @throws IOException                   if any file operation fails
     * @throws UnsupportedOperationException if {@code paperSize} is {@link PaperSize#CUSTOM}
     */
    public static void write(
            List<Signature> signatures,
            Map<String, Path> sourceDocPaths,
            Path outputPath,
            PaperSize paperSize,
            MarkConfig markConfig,
            NumberingConfig numberingConfig) throws IOException {
        write(signatures, sourceDocPaths, outputPath, paperSize, markConfig, numberingConfig, null);
    }

    /**
     * Writes the imposed output PDF sourcing pages from multiple source documents,
     * with optional creep compensation.
     *
     * <p>When {@code creepConfig} is non-null and {@link CreepConfig#isApplyToOutput()} is
     * true, page content on each sheet is shifted inward toward the fold line by the
     * per-sheet creep amount, compensating for the outward protrusion of inner sheets.
     *
     * @param signatures       composed signatures; must not be null
     * @param sourceDocPaths   map from document ID to file path; may be null or empty
     * @param outputPath       destination path; must not be null
     * @param paperSize        book page size; must not be null; {@code CUSTOM} is unsupported
     * @param markConfig       controls which output marks are rendered; null disables all marks
     * @param numberingConfig  controls folio rendering; null disables folio output
     * @param creepConfig      creep compensation settings; null or {@code applyToOutput=false}
     *                         disables compensation
     * @throws IOException                   if any file operation fails
     * @throws UnsupportedOperationException if {@code paperSize} is {@link PaperSize#CUSTOM}
     */
    public static void write(
            List<Signature> signatures,
            Map<String, Path> sourceDocPaths,
            Path outputPath,
            PaperSize paperSize,
            MarkConfig markConfig,
            NumberingConfig numberingConfig,
            CreepConfig creepConfig) throws IOException {
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(paperSize, "paperSize");

        MarkConfig marks = markConfig != null ? markConfig : MarkConfig.builder().build();
        PDRectangle bookRect = bookPageRect(paperSize);
        PDRectangle sheetRect = new PDRectangle(bookRect.getWidth() * 2, bookRect.getHeight());
        int totalSigs = signatures.size();

        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        Map<String, PDDocument> openDocs = openSourceDocs(sourceDocPaths);
        try {
            try (PDDocument outDoc = new PDDocument()) {
                LayerUtility layers = new LayerUtility(outDoc);
                for (Signature sig : signatures) {
                    int sheetsInSig = sig.getSheets().size();
                    for (ImposedSheet sheet : sig.getSheets()) {
                        addSheetPage(outDoc, layers, openDocs, sheetRect, bookRect,
                                sheet.getFrontPages(), marks, numberingConfig, font,
                                sheet.getSheetIndex(), sheetsInSig,
                                sig.getSignatureIndex(), totalSigs, creepConfig);
                        addSheetPage(outDoc, layers, openDocs, sheetRect, bookRect,
                                sheet.getBackPages(), marks, numberingConfig, font,
                                sheet.getSheetIndex(), sheetsInSig,
                                sig.getSignatureIndex(), totalSigs, creepConfig);
                    }
                }
                outDoc.save(outputPath.toFile());
            }
        } finally {
            closeAll(openDocs);
        }
    }

    /**
     * Writes an imposed PDF for Group A (Perfect Binding, Spiral, Japanese Stab).
     *
     * <p>Group A has no folding — each physical sheet carries exactly one source page on
     * the front and one on the back. The output PDF therefore contains one full-size
     * portrait page per source page, in reading order. Blank filler pages added by the
     * padding step are omitted from the output.
     *
     * @param signatures      composed signatures; must not be null
     * @param sourceDocPaths  map from document ID to file path; may be null or empty
     * @param outputPath      destination path; must not be null
     * @param paperSize       book page size; must not be null
     * @param markConfig      output marks; null disables all marks
     * @param numberingConfig folio rendering; null disables folios
     * @param creepConfig     creep compensation; null or {@code applyToOutput=false} skips
     * @throws IOException if any file operation fails
     */
    public static void writeGroupA(
            List<Signature> signatures,
            Map<String, Path> sourceDocPaths,
            Path outputPath,
            PaperSize paperSize,
            MarkConfig markConfig,
            NumberingConfig numberingConfig,
            CreepConfig creepConfig) throws IOException {
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(paperSize, "paperSize");

        MarkConfig marks = markConfig != null ? markConfig : MarkConfig.builder().build();

        // Collect all content/aesthetic pages from every sheet across all signatures,
        // then sort by physicalPosition to restore reading order.
        List<QuirePage> pages = new ArrayList<>();
        for (Signature sig : signatures) {
            for (ImposedSheet sheet : sig.getSheets()) {
                pages.addAll(sheet.getFrontPages());
                pages.addAll(sheet.getBackPages());
            }
        }
        pages.sort(Comparator.comparingInt(QuirePage::getPhysicalPosition));
        pages.removeIf(p -> p.getPageType() == PageType.FILLER_BLANK
                         || p.getPageType() == PageType.COMPLETION_BLANK);

        PDRectangle bookRect = bookPageRect(paperSize);
        float pageW = bookRect.getWidth();
        float pageH = bookRect.getHeight();
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        Map<String, PDDocument> openDocs = openSourceDocs(sourceDocPaths);
        try {
            try (PDDocument outDoc = new PDDocument()) {
                for (QuirePage page : pages) {
                    PDPage outPage = new PDPage(bookRect);
                    outDoc.addPage(outPage);
                    try (PDPageContentStream cs = new PDPageContentStream(outDoc, outPage)) {
                        placePageFullSize(outDoc, new LayerUtility(outDoc), cs,
                            openDocs, page, pageW, pageH);
                        if (marks.isTrimLines()) {
                            renderTrimLines(cs, 0f, pageW, pageH);
                        }
                        if (numberingConfig != null) {
                            renderFolio(cs, page, numberingConfig, font, 0f, pageW, pageH);
                        }
                    }
                }
                outDoc.save(outputPath.toFile());
            }
        } finally {
            closeAll(openDocs);
        }
    }

    private static void placePageFullSize(
            PDDocument outDoc,
            LayerUtility layers,
            PDPageContentStream cs,
            Map<String, PDDocument> srcDocs,
            QuirePage page,
            float pageW,
            float pageH) throws IOException {
        if (page.getPageType() != PageType.CONTENT
                && page.getPageType() != PageType.AESTHETIC) {
            return;
        }
        if (page.getSourcePageIndex().isEmpty()) {
            return;
        }
        String docId = page.getSourceDocumentId().orElse(null);
        PDDocument srcDoc = docId != null ? srcDocs.get(docId) : null;
        if (srcDoc == null) {
            return;
        }
        int srcIdx = page.getSourcePageIndex().get();
        PDFormXObject form = layers.importPageAsForm(srcDoc, srcIdx);
        PDRectangle srcBox = srcDoc.getPage(srcIdx).getMediaBox();
        float scale = Math.min(pageW / srcBox.getWidth(), pageH / srcBox.getHeight());
        float tx = (pageW - srcBox.getWidth() * scale) / 2f;
        float ty = (pageH - srcBox.getHeight() * scale) / 2f;
        cs.saveGraphicsState();
        cs.transform(new Matrix(scale, 0, 0, scale, tx, ty));
        cs.drawForm(form);
        cs.restoreGraphicsState();
    }

    private static Map<String, PDDocument> openSourceDocs(Map<String, Path> sourceDocPaths)
            throws IOException {
        Map<String, PDDocument> docs = new LinkedHashMap<>();
        if (sourceDocPaths != null) {
            for (Map.Entry<String, Path> entry : sourceDocPaths.entrySet()) {
                docs.put(entry.getKey(), Loader.loadPDF(entry.getValue().toFile()));
            }
        }
        return docs;
    }

    private static void closeAll(Map<String, PDDocument> docs) {
        for (PDDocument doc : docs.values()) {
            try {
                doc.close();
            } catch (IOException ignored) {
                // COVERAGE-EXCLUDE: PDFBox PDDocument.close() does not throw in practice
            }
        }
    }

    private static void addSheetPage(
            PDDocument outDoc,
            LayerUtility layers,
            Map<String, PDDocument> srcDocs,
            PDRectangle sheetRect,
            PDRectangle bookRect,
            List<QuirePage> pages,
            MarkConfig marks,
            NumberingConfig numberingConfig,
            PDFont font,
            int sheetIndex,
            int sheetsInSig,
            int sigIndex,
            int totalSigs,
            CreepConfig creepConfig) throws IOException {
        PDPage outPage = new PDPage(sheetRect);
        outDoc.addPage(outPage);
        float halfW = bookRect.getWidth();
        float halfH = bookRect.getHeight();
        float creepShiftPt = (creepConfig != null && creepConfig.isApplyToOutput())
                ? CreepTransformApplier.shiftPt(sheetIndex, creepConfig)
                : 0f;
        try (PDPageContentStream cs = new PDPageContentStream(outDoc, outPage)) {
            placePageOnSheet(outDoc, layers, cs, srcDocs, pages.get(0), 0f, halfW, halfH, creepShiftPt);
            placePageOnSheet(outDoc, layers, cs, srcDocs, pages.get(1), halfW, halfW, halfH, creepShiftPt);
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
            Map<String, PDDocument> srcDocs,
            QuirePage page,
            float xOffset,
            float halfW,
            float halfH,
            float creepShiftPt) throws IOException {
        if (page.getPageType() != PageType.CONTENT) {
            return;
        }
        if (page.getSourcePageIndex().isEmpty()) {
            return;
        }
        String docId = page.getSourceDocumentId().orElse(null);
        PDDocument srcDoc = docId != null ? srcDocs.get(docId) : null;
        if (srcDoc == null) {
            return;
        }
        int srcIdx = page.getSourcePageIndex().get();
        PDFormXObject form = layers.importPageAsForm(srcDoc, srcIdx);
        PDRectangle srcBox = srcDoc.getPage(srcIdx).getMediaBox();
        float scale = Math.min(halfW / srcBox.getWidth(), halfH / srcBox.getHeight());
        // Shift left-half pages toward the fold (right), right-half pages toward the fold (left)
        float creepOffset = xOffset < halfW ? creepShiftPt : -creepShiftPt;
        float tx = xOffset + (halfW - srcBox.getWidth() * scale) / 2f + creepOffset;
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
        float mmToPt = 72f / 25.4f;
        float endMarginPt = (float) (sewingConfig.getEndMarginMm() * mmToPt);
        float y0 = endMarginPt;
        float y1 = halfH - endMarginPt;
        float dotSize = 3f;
        float x = halfW - dotSize / 2f;
        cs.saveGraphicsState();
        cs.setNonStrokingColor(Color.BLACK);
        if (sewingConfig.getStyle() == SewingConfig.SewingStyle.BANDED) {
            renderBandedHoles(cs, x, y0, y1, dotSize, sewingConfig, mmToPt);
        } else {
            renderSimpleHoles(cs, x, y0, y1, dotSize, sewingConfig.getHoleCount());
        }
        cs.restoreGraphicsState();
    }

    private static void renderSimpleHoles(
            PDPageContentStream cs, float x, float y0, float y1,
            float dotSize, int numHoles) throws IOException {
        for (int i = 0; i < numHoles; i++) {
            float y = y0 + (float) i / (numHoles - 1) * (y1 - y0);
            cs.addRect(x, y, dotSize, dotSize);
            cs.fill();
        }
    }

    private static void renderBandedHoles(
            PDPageContentStream cs, float x, float y0, float y1,
            float dotSize, SewingConfig cfg, float mmToPt) throws IOException {
        int bands = cfg.getBandCount();
        float halfBandPt = (float) (cfg.getBandWidthMm() * mmToPt) / 2f;
        cs.addRect(x, y0, dotSize, dotSize);
        cs.fill();
        for (int k = 1; k <= bands; k++) {
            float center = y0 + (float) k / (bands + 1) * (y1 - y0);
            cs.addRect(x, center - halfBandPt, dotSize, dotSize);
            cs.fill();
            cs.addRect(x, center + halfBandPt, dotSize, dotSize);
            cs.fill();
        }
        cs.addRect(x, y1, dotSize, dotSize);
        cs.fill();
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
        if (page.getPageZone().isPresent()) {
            FolioStyle zoneStyle = switch (page.getPageZone().get()) {
                case FRONT_MATTER -> numberingConfig.getFrontMatterStyle();
                case BODY         -> numberingConfig.getBodyStyle();
                case REAR_MATTER  -> numberingConfig.getRearMatterStyle();
            };
            if (zoneStyle == FolioStyle.NONE) {
                return;
            }
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
