/*
 * Copyright 2025 Quire Contributors
 *
 * This file is part of Quire.
 *
 * Quire is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Quire is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Quire.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.quire.core.pdf;

import com.quire.core.model.ImposedSheet;
import com.quire.core.model.PaperSize;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;
import com.quire.core.model.Signature;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

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
     * Writes the imposed output PDF to {@code outputPath}.
     *
     * @param signatures    composed signatures from the imposition engine; must not be null
     * @param sourcePdfPath path to the source PDF, or {@code null} if all pages are blank
     * @param outputPath    destination path for the imposed PDF; must not be null
     * @param paperSize     book page size; must not be null; {@code CUSTOM} is unsupported
     * @throws IOException                   if any file operation fails
     * @throws UnsupportedOperationException if {@code paperSize} is {@link PaperSize#CUSTOM}
     */
    public static void write(
            List<Signature> signatures,
            Path sourcePdfPath,
            Path outputPath,
            PaperSize paperSize) throws IOException {
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(paperSize, "paperSize");

        PDRectangle bookRect = bookPageRect(paperSize);
        PDRectangle sheetRect = new PDRectangle(bookRect.getWidth() * 2, bookRect.getHeight());

        PDDocument srcDoc = sourcePdfPath != null ? Loader.loadPDF(sourcePdfPath.toFile()) : null;
        try {
            try (PDDocument outDoc = new PDDocument()) {
                LayerUtility layers = new LayerUtility(outDoc);
                for (Signature sig : signatures) {
                    for (ImposedSheet sheet : sig.getSheets()) {
                        addSheetPage(outDoc, layers, srcDoc, sheetRect, bookRect,
                                sheet.getFrontPages());
                        addSheetPage(outDoc, layers, srcDoc, sheetRect, bookRect,
                                sheet.getBackPages());
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
            List<QuirePage> pages) throws IOException {
        PDPage outPage = new PDPage(sheetRect);
        outDoc.addPage(outPage);
        try (PDPageContentStream cs = new PDPageContentStream(outDoc, outPage)) {
            float halfW = bookRect.getWidth();
            float halfH = bookRect.getHeight();
            placePageOnSheet(outDoc, layers, cs, srcDoc, pages.get(0), 0f, halfW, halfH);
            placePageOnSheet(outDoc, layers, cs, srcDoc, pages.get(1), halfW, halfW, halfH);
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
