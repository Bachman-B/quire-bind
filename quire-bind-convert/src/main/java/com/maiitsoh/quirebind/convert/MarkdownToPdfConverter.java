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

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.maiitsoh.quirebind.convert.PdfPageRenderer.Block;
import com.maiitsoh.quirebind.convert.PdfPageRenderer.Block.BlockType;
import com.maiitsoh.quirebind.core.model.PaperSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts a Markdown file (.md / .markdown) to a paginated PDF using PDFBox.
 *
 * <p>Parses CommonMark via flexmark-java and renders headings, paragraphs, and
 * lists using {@link PdfPageRenderer}.
 */
public final class MarkdownToPdfConverter {

    private static final Parser PARSER =
        Parser.builder(new MutableDataSet()).build();

    private MarkdownToPdfConverter() {
    }

    /**
     * Converts a Markdown file to a PDF written to a new temp file.
     *
     * @param mdFile    path to the source Markdown file; must not be null
     * @param paperSize target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if reading the Markdown or writing the PDF fails
     */
    public static Path convert(Path mdFile, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(mdFile, "mdFile");
        Objects.requireNonNull(paperSize, "paperSize");
        String markdown = Files.readString(mdFile);
        return convertMarkdown(markdown, paperSize);
    }

    /**
     * Converts a Markdown string to a PDF written to a new temp file.
     *
     * @param markdown  the Markdown source; must not be null
     * @param paperSize target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    public static Path convertMarkdown(String markdown, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(markdown, "markdown");
        Objects.requireNonNull(paperSize, "paperSize");
        Document doc = PARSER.parse(markdown);
        List<Block> blocks = extractBlocks(doc);
        return PdfPageRenderer.render(blocks, paperSize);
    }

    private static List<Block> extractBlocks(Node root) {
        List<Block> blocks = new ArrayList<>();
        for (Node child : root.getChildren()) {
            if (child instanceof Heading h) {
                String text = h.getText().toString().trim();
                if (!text.isEmpty()) {
                    blocks.add(new Block(BlockType.HEADING, text, h.getLevel()));
                    blocks.add(new Block(BlockType.BLANK, "", 0));
                }
            } else if (child instanceof Paragraph p) {
                String text = flatText(p);
                if (!text.isEmpty()) {
                    blocks.add(new Block(BlockType.PARAGRAPH, text, 0));
                    blocks.add(new Block(BlockType.BLANK, "", 0));
                }
            } else if (child instanceof BulletList || child instanceof OrderedList) {
                for (Node item : child.getChildren()) {
                    if (item instanceof BulletListItem || item instanceof OrderedListItem) {
                        // First paragraph inside the list item
                        Node first = item.getFirstChild();
                        String text = first != null ? flatText(first) : "";
                        if (!text.isEmpty()) {
                            blocks.add(new Block(BlockType.LIST_ITEM, text, 0));
                        }
                    }
                }
                blocks.add(new Block(BlockType.BLANK, "", 0));
            } else {
                // Recurse into block containers
                blocks.addAll(extractBlocks(child));
            }
        }
        return blocks;
    }

    private static String flatText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child : node.getChildren()) {
            if (child instanceof SoftLineBreak) {
                sb.append(' ');
            } else if (child.getFirstChild() == null) {
                sb.append(child.getChars().toString());
            } else {
                sb.append(flatText(child));
            }
        }
        return sb.toString().trim();
    }
}
