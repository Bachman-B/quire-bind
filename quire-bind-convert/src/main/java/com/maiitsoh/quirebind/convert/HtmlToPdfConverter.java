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

import com.maiitsoh.quirebind.convert.PdfPageRenderer.Block;
import com.maiitsoh.quirebind.convert.PdfPageRenderer.Block.BlockType;
import com.maiitsoh.quirebind.core.model.PaperSize;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Converts an HTML file to a paginated PDF using PDFBox.
 *
 * <p>Parses the HTML via jsoup and extracts headings, paragraphs, and lists,
 * then renders them with {@link PdfPageRenderer}. Full CSS layout is not supported;
 * visual fidelity is sufficient for bookbinding preparation.
 */
public final class HtmlToPdfConverter {

    private static final Set<String> HEADING_TAGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
    private static final Set<String> BLOCK_TAGS =
        Set.of("p", "div", "section", "article", "header", "footer", "main",
               "blockquote", "pre", "figure", "figcaption", "address");
    private static final Set<String> LIST_TAGS = Set.of("ul", "ol");

    private HtmlToPdfConverter() {
    }

    /**
     * Converts an HTML file to a PDF written to a new temp file.
     *
     * @param htmlFile  path to the source HTML file; must not be null
     * @param paperSize target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if reading the HTML or writing the PDF fails
     */
    public static Path convert(Path htmlFile, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(htmlFile, "htmlFile");
        Objects.requireNonNull(paperSize, "paperSize");
        String html = Files.readString(htmlFile);
        return convertHtml(html, paperSize);
    }

    /**
     * Converts an HTML string to a PDF written to a new temp file.
     *
     * @param html      the HTML content; must not be null
     * @param paperSize target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    public static Path convertHtml(String html, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(html, "html");
        Objects.requireNonNull(paperSize, "paperSize");
        Document doc = Jsoup.parse(html);
        List<Block> blocks = extractBlocks(doc.body() != null ? doc.body() : doc);
        return PdfPageRenderer.render(blocks, paperSize);
    }

    private static List<Block> extractBlocks(Element root) {
        List<Block> blocks = new ArrayList<>();
        for (Element el : root.children()) {
            String tag = el.tagName().toLowerCase();
            if (HEADING_TAGS.contains(tag)) {
                int level = tag.charAt(1) - '0';
                String text = el.text().trim();
                if (!text.isEmpty()) {
                    blocks.add(new Block(BlockType.HEADING, text, level));
                    blocks.add(new Block(BlockType.BLANK, "", 0));
                }
            } else if (LIST_TAGS.contains(tag)) {
                for (Element li : el.select("> li")) {
                    String text = li.text().trim();
                    if (!text.isEmpty()) {
                        blocks.add(new Block(BlockType.LIST_ITEM, text, 0));
                    }
                }
                blocks.add(new Block(BlockType.BLANK, "", 0));
            } else if (BLOCK_TAGS.contains(tag)) {
                String text = el.text().trim();
                if (!text.isEmpty()) {
                    blocks.add(new Block(BlockType.PARAGRAPH, text, 0));
                    blocks.add(new Block(BlockType.BLANK, "", 0));
                }
            } else {
                // Recurse into container elements
                blocks.addAll(extractBlocks(el));
            }
        }
        // If nothing extracted, fall back to full text
        if (blocks.isEmpty()) {
            String text = root.text().trim();
            if (!text.isEmpty()) {
                for (String para : text.split("\n\n+")) {
                    if (!para.isBlank()) {
                        blocks.add(new Block(BlockType.PARAGRAPH, para.trim(), 0));
                        blocks.add(new Block(BlockType.BLANK, "", 0));
                    }
                }
            }
        }
        return blocks;
    }
}
