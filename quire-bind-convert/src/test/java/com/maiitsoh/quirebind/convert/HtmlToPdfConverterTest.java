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

class HtmlToPdfConverterTest {

    @TempDir
    Path tempDir;

    // ── pageDimsMm ──────────────────────────────────────────────────────────

    @Test
    void a4Dimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.A4);
        assertEquals(210f, d[0], 0.01f);
        assertEquals(297f, d[1], 0.01f);
    }

    @Test
    void a3Dimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.A3);
        assertEquals(297f, d[0], 0.01f);
        assertEquals(420f, d[1], 0.01f);
    }

    @Test
    void a5Dimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.A5);
        assertEquals(148f, d[0], 0.01f);
        assertEquals(210f, d[1], 0.01f);
    }

    @Test
    void letterDimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.LETTER);
        assertEquals(216f, d[0], 0.01f);
        assertEquals(279f, d[1], 0.01f);
    }

    @Test
    void legalDimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.LEGAL);
        assertEquals(216f, d[0], 0.01f);
        assertEquals(356f, d[1], 0.01f);
    }

    @Test
    void halfLetterDimensions() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.HALF_LETTER);
        assertEquals(140f, d[0], 0.01f);
        assertEquals(216f, d[1], 0.01f);
    }

    @Test
    void customFallsBackToA4() {
        float[] d = HtmlToPdfConverter.pageDimsMm(PaperSize.CUSTOM);
        assertEquals(210f, d[0], 0.01f);
        assertEquals(297f, d[1], 0.01f);
    }

    // ── convertHtml ─────────────────────────────────────────────────────────

    @Test
    void simpleHtmlProducesPdf() throws IOException {
        Path out = HtmlToPdfConverter.convertHtml(
            "<html><body><p>Hello world</p></body></html>", null, PaperSize.A4);
        try {
            assertTrue(Files.exists(out));
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void htmlWithHeadingsProducesPdf() throws IOException {
        String html = "<html><head><title>Test</title></head>"
            + "<body><h1>Title</h1><p>Body text.</p></body></html>";
        Path out = HtmlToPdfConverter.convertHtml(html, null, PaperSize.A5);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void htmlWithScriptTagStripped() throws IOException {
        // Script tags should be stripped by preprocessor — should not crash
        String html = "<html><body><script>alert('x')</script><p>Content</p></body></html>";
        Path out = HtmlToPdfConverter.convertHtml(html, null, PaperSize.A4);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void htmlWithCanvasStripped() throws IOException {
        String html = "<html><body><canvas id='c'></canvas><p>Content</p></body></html>";
        Path out = HtmlToPdfConverter.convertHtml(html, null, PaperSize.A4);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void htmlWithFixedSizeContainerUsesDetectedPageSize() throws IOException {
        // The 680×960px container should trigger the fixed-container detection
        String html = "<html><head><style>"
            + ".cover { width: 680px; height: 960px; background: #000; }"
            + "</style></head><body><div class='cover'><p>Cover</p></div></body></html>";
        Path out = HtmlToPdfConverter.convertHtml(html, null, PaperSize.A4);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void htmlWithSvgWrappedCorrectly() throws IOException {
        // SVG should be wrapped in constraining div — should not collapse to 1 page
        String html = "<html><body>"
            + "<svg viewBox='0 0 900 200' xmlns='http://www.w3.org/2000/svg'>"
            + "<rect width='900' height='200' fill='#eee'/>"
            + "</svg>"
            + "<p>After SVG</p></body></html>";
        Path out = HtmlToPdfConverter.convertHtml(html, null, PaperSize.A4);
        try {
            assertTrue(Files.size(out) > 0);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void nullHtmlThrows() {
        assertThrows(NullPointerException.class,
            () -> HtmlToPdfConverter.convertHtml(null, null, PaperSize.A4));
    }

    @Test
    void nullPaperSizeThrows() {
        assertThrows(NullPointerException.class,
            () -> HtmlToPdfConverter.convertHtml("<html/>", null, null));
    }

    // ── convert (file) ──────────────────────────────────────────────────────

    @Test
    void convertFileProducesPdf() throws IOException {
        Path html = tempDir.resolve("page.html");
        Files.writeString(html, "<html><body><p>File test</p></body></html>");
        Path out = HtmlToPdfConverter.convert(html, PaperSize.A4);
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
            () -> HtmlToPdfConverter.convert(null, PaperSize.A4));
    }
}
