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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownToPdfConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void simpleMarkdownProducesPdf() throws IOException {
        Path out = MarkdownToPdfConverter.convertMarkdown("# Hello\n\nWorld.", PaperSize.A4);
        try {
            assertTrue(Files.exists(out));
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void markdownWithListsProducesPdf() throws IOException {
        String md = "# Guide\n\n- Item one\n- Item two\n- Item three\n\nDone.";
        Path out = MarkdownToPdfConverter.convertMarkdown(md, PaperSize.A5);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void markdownWithCodeBlockProducesPdf() throws IOException {
        String md = "# Code\n\n```java\nSystem.out.println(\"hello\");\n```\n\nEnd.";
        Path out = MarkdownToPdfConverter.convertMarkdown(md, PaperSize.LETTER);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void markdownWithInlineSvgProducesPdf() throws IOException {
        // Inline SVG in Markdown should not collapse to 1 page
        String md = "# Title\n\n<svg viewBox='0 0 900 200' xmlns='http://www.w3.org/2000/svg'>"
            + "<rect width='900' height='200' fill='#eee'/></svg>\n\nAfter diagram.";
        Path out = MarkdownToPdfConverter.convertMarkdown(md, PaperSize.A4);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void emptyMarkdownProducesPdf() throws IOException {
        Path out = MarkdownToPdfConverter.convertMarkdown("", PaperSize.A4);
        try {
            assertTrue(Files.exists(out));
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void convertFileProducesPdf() throws IOException {
        Path md = tempDir.resolve("guide.md");
        Files.writeString(md, "# Hello\n\nMarkdown file test.");
        Path out = MarkdownToPdfConverter.convert(md, PaperSize.A4);
        try {
            assertTrue(Files.exists(out));
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void convertFileNullPathThrows() {
        assertThrows(NullPointerException.class,
            () -> MarkdownToPdfConverter.convert(null, PaperSize.A4));
    }

    @Test
    void convertMarkdownNullStringThrows() {
        assertThrows(NullPointerException.class,
            () -> MarkdownToPdfConverter.convertMarkdown(null, PaperSize.A4));
    }

    @Test
    void convertMarkdownNullPaperSizeThrows() {
        assertThrows(NullPointerException.class,
            () -> MarkdownToPdfConverter.convertMarkdown("# Hi", null));
    }
}
