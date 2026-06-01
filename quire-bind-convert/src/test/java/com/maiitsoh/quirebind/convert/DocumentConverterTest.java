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

class DocumentConverterTest {

    @TempDir
    Path tempDir;

    // ── isSupported ─────────────────────────────────────────────────────────

    @Test
    void pdfIsSupported() {
        assertTrue(DocumentConverter.isSupported("document.pdf"));
    }

    @Test
    void htmlIsSupported() {
        assertTrue(DocumentConverter.isSupported("page.html"));
    }

    @Test
    void htmIsSupported() {
        assertTrue(DocumentConverter.isSupported("page.htm"));
    }

    @Test
    void mdIsSupported() {
        assertTrue(DocumentConverter.isSupported("notes.md"));
    }

    @Test
    void markdownIsSupported() {
        assertTrue(DocumentConverter.isSupported("guide.markdown"));
    }

    @Test
    void docxIsNotSupported() {
        assertFalse(DocumentConverter.isSupported("doc.docx"));
    }

    @Test
    void txtIsNotSupported() {
        assertFalse(DocumentConverter.isSupported("readme.txt"));
    }

    @Test
    void nullIsNotSupported() {
        assertFalse(DocumentConverter.isSupported(null));
    }

    @Test
    void extensionCheckIsCaseInsensitive() {
        assertTrue(DocumentConverter.isSupported("cover.PDF"));
        assertTrue(DocumentConverter.isSupported("guide.MD"));
        assertTrue(DocumentConverter.isSupported("page.HTML"));
    }

    @Test
    void isSupportedReturnsFalseForEmptyString() {
        assertFalse(DocumentConverter.isSupported(""));
    }

    // ── toPdf ───────────────────────────────────────────────────────────────

    @Test
    void pdfFileIsReturnedUnchanged() throws IOException {
        Path pdf = tempDir.resolve("source.pdf");
        Files.writeString(pdf, "%PDF-1.4");
        Path result = DocumentConverter.toPdf(pdf, PaperSize.A4);
        assertEquals(pdf, result);
    }

    @Test
    void htmlFileIsConvertedToPdf() throws IOException {
        Path html = tempDir.resolve("page.html");
        Files.writeString(html, "<html><body><p>Hello</p></body></html>");
        Path result = DocumentConverter.toPdf(html, PaperSize.A4);
        try {
            assertTrue(Files.exists(result));
            assertTrue(Files.size(result) > 0);
            assertNotEquals(html, result);
        } finally {
            Files.deleteIfExists(result);
        }
    }

    @Test
    void markdownFileIsConvertedToPdf() throws IOException {
        Path md = tempDir.resolve("guide.md");
        Files.writeString(md, "# Hello\n\nWorld.");
        Path result = DocumentConverter.toPdf(md, PaperSize.A4);
        try {
            assertTrue(Files.exists(result));
            assertTrue(Files.size(result) > 0);
            assertNotEquals(md, result);
        } finally {
            Files.deleteIfExists(result);
        }
    }

    @Test
    void htmFileIsConvertedToPdf() throws IOException {
        Path html = tempDir.resolve("page.htm");
        Files.writeString(html, "<html><body><p>Hello</p></body></html>");
        Path result = DocumentConverter.toPdf(html, PaperSize.A4);
        try {
            assertTrue(Files.exists(result));
        } finally {
            Files.deleteIfExists(result);
        }
    }

    @Test
    void markdownExtensionFileIsConvertedToPdf() throws IOException {
        Path md = tempDir.resolve("guide.markdown");
        Files.writeString(md, "# Hello\n\nWorld.");
        Path result = DocumentConverter.toPdf(md, PaperSize.A4);
        try {
            assertTrue(Files.exists(result));
        } finally {
            Files.deleteIfExists(result);
        }
    }

    @Test
    void unsupportedExtensionThrows() {
        Path txt = tempDir.resolve("readme.txt");
        assertThrows(UnsupportedOperationException.class,
            () -> DocumentConverter.toPdf(txt, PaperSize.A4));
    }

    @Test
    void nullSourceFileThrows() {
        assertThrows(NullPointerException.class,
            () -> DocumentConverter.toPdf(null, PaperSize.A4));
    }

    @Test
    void nullPaperSizeThrows() throws IOException {
        Path html = tempDir.resolve("page.html");
        Files.writeString(html, "<html><body></body></html>");
        assertThrows(NullPointerException.class,
            () -> DocumentConverter.toPdf(html, null));
    }
}
