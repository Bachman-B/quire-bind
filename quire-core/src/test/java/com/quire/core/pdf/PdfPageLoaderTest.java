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

import com.quire.core.model.PageSequence;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfPageLoaderTest {

    @TempDir
    Path tempDir;

    private Path createPdf(int pageCount) throws IOException {
        Path out = tempDir.resolve("source.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.save(out.toFile());
        }
        return out;
    }

    @Test
    void nullPathThrows() {
        assertThrows(NullPointerException.class, () -> PdfPageLoader.load(null));
    }

    @Test
    void nonExistentPathThrowsIoException() {
        assertThrows(IOException.class,
                () -> PdfPageLoader.load(Path.of("/nonexistent/missing.pdf")));
    }

    @Test
    void emptyPdfReturnsEmptySequence() throws IOException {
        Path pdf = createPdf(0);
        PageSequence seq = PdfPageLoader.load(pdf);
        assertEquals(0, seq.pageCount());
    }

    @Test
    void fourPagePdfReturnsSequenceOfFour() throws IOException {
        Path pdf = createPdf(4);
        PageSequence seq = PdfPageLoader.load(pdf);
        assertEquals(4, seq.pageCount());
    }

    @Test
    void pageTypeIsContent() throws IOException {
        Path pdf = createPdf(2);
        PageSequence seq = PdfPageLoader.load(pdf);
        for (QuirePage page : seq.getPages()) {
            assertEquals(PageType.CONTENT, page.getPageType());
        }
    }

    @Test
    void sourceDocumentIdIsFilePath() throws IOException {
        Path pdf = createPdf(1);
        PageSequence seq = PdfPageLoader.load(pdf);
        assertEquals(pdf.toString(), seq.getPages().get(0).getSourceDocumentId().orElseThrow());
    }

    @Test
    void sourcePageIndexMatchesPosition() throws IOException {
        Path pdf = createPdf(3);
        PageSequence seq = PdfPageLoader.load(pdf);
        List<QuirePage> pages = seq.getPages();
        for (int i = 0; i < pages.size(); i++) {
            assertEquals(i, pages.get(i).getSourcePageIndex().orElseThrow());
        }
    }

    @Test
    void physicalPositionsAreSequential() throws IOException {
        Path pdf = createPdf(3);
        PageSequence seq = PdfPageLoader.load(pdf);
        List<QuirePage> pages = seq.getPages();
        for (int i = 0; i < pages.size(); i++) {
            assertEquals(i, pages.get(i).getPhysicalPosition());
        }
    }
}
