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
package com.maiitsoh.quirebind.convert;

import com.maiitsoh.quirebind.core.model.PaperSize;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a structured document (list of {@link Block} elements) to a paginated PDF
 * using Apache PDFBox. Handles paragraph flow, headings, and bulleted/numbered lists.
 *
 * <p>This renderer produces a clean, print-ready PDF from structured content without
 * depending on any external HTML rendering library.
 */
final class PdfPageRenderer {

    /** A single content element in the document. */
    record Block(BlockType type, String text, int level) {
        enum BlockType { PARAGRAPH, HEADING, LIST_ITEM, BLANK }
    }

    private static final float MM_TO_PT = 72f / 25.4f;
    private static final float MARGIN_MM = 20f;
    private static final float LINE_SPACING = 1.4f;

    private PdfPageRenderer() {
    }

    /**
     * Renders the given blocks to a new PDF temp file.
     *
     * @param blocks    the document content to render; must not be null
     * @param paperSize the target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    static Path render(List<Block> blocks, PaperSize paperSize) throws IOException {
        PDRectangle pageRect = pageRect(paperSize);
        float margin = MARGIN_MM * MM_TO_PT;
        float pageW = pageRect.getWidth();
        float pageH = pageRect.getHeight();
        float contentW = pageW - 2 * margin;
        float contentH = pageH - 2 * margin;

        PDFont fontNormal = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        PDFont fontBold   = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

        Path out = Files.createTempFile("quire-convert-", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            float y = contentH;
            PDPage page = new PDPage(pageRect);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            for (Block block : blocks) {
                if (block.type() == Block.BlockType.BLANK) {
                    y -= 6f;
                    if (y < 0) {
                        cs.close();
                        page = new PDPage(pageRect);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = contentH;
                    }
                    continue;
                }

                float fontSize = fontSize(block);
                float leading = fontSize * LINE_SPACING;
                float indentPt = block.type() == Block.BlockType.LIST_ITEM ? 14f : 0f;
                String prefix = block.type() == Block.BlockType.LIST_ITEM ? "• " : "";
                PDFont font = (block.type() == Block.BlockType.HEADING) ? fontBold : fontNormal;

                List<String> lines = wrap(prefix + block.text(), font, fontSize,
                    contentW - indentPt);

                if (block.type() == Block.BlockType.HEADING) {
                    y -= leading * 0.4f; // extra space before heading
                }

                for (String line : lines) {
                    if (y - leading < 0) {
                        cs.close();
                        page = new PDPage(pageRect);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = contentH;
                    }
                    y -= leading;
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin + indentPt, margin + y);
                    cs.showText(line);
                    cs.endText();
                }

                if (block.type() == Block.BlockType.HEADING) {
                    y -= leading * 0.2f; // extra space after heading
                }
            }

            cs.close();
            doc.save(out.toFile());
        } catch (IOException e) {
            Files.deleteIfExists(out);
            throw e;
        }
        return out;
    }

    private static float fontSize(Block block) {
        if (block.type() != Block.BlockType.HEADING) {
            return 11f;
        }
        return switch (block.level()) {
            case 1  -> 20f;
            case 2  -> 16f;
            case 3  -> 13f;
            default -> 11.5f;
        };
    }

    private static List<String> wrap(String text, PDFont font, float fontSize, float maxWidth)
            throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ", -1);
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float w = font.getStringWidth(sanitize(candidate)) / 1000f * fontSize;
            if (w > maxWidth && !current.isEmpty()) {
                lines.add(sanitize(current.toString()));
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(sanitize(current.toString()));
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    /** Remove characters outside the WinAnsiEncoding range that PDFBox Type1 fonts can't handle. */
    private static String sanitize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= 0x20 && c < 0x100) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static PDRectangle pageRect(PaperSize size) {
        return switch (size) {
            case A3          -> PDRectangle.A3;
            case A4          -> PDRectangle.A4;
            case A5          -> PDRectangle.A5;
            case LETTER      -> PDRectangle.LETTER;
            case LEGAL       -> PDRectangle.LEGAL;
            case HALF_LETTER -> new PDRectangle(396f, 612f);
            case CUSTOM      -> PDRectangle.A4;
        };
    }
}
