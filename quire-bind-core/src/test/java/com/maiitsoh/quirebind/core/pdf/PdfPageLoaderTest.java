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

import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.QuirePage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

    @Test
    void loadAllEmptyListReturnsEmptySequence() throws IOException {
        PageSequence seq = PdfPageLoader.loadAll(List.of());
        assertEquals(0, seq.pageCount());
    }

    @Test
    void loadAllNullListThrows() {
        assertThrows(NullPointerException.class, () -> PdfPageLoader.loadAll(null));
    }

    @Test
    void loadAllSinglePdfMatchesLoad() throws IOException {
        Path pdf = createPdf(4);
        PageSequence single = PdfPageLoader.load(pdf);
        PageSequence all = PdfPageLoader.loadAll(List.of(pdf));
        assertEquals(single.pageCount(), all.pageCount());
        for (int i = 0; i < all.pageCount(); i++) {
            assertEquals(single.getPages().get(i).getSourceDocumentId(),
                    all.getPages().get(i).getSourceDocumentId());
            assertEquals(single.getPages().get(i).getSourcePageIndex(),
                    all.getPages().get(i).getSourcePageIndex());
        }
    }

    @Test
    void loadAllConcatenatesInOrder() throws IOException {
        Path pdf1 = tempDir.resolve("first.pdf");
        Path pdf2 = tempDir.resolve("second.pdf");
        try (PDDocument d1 = new PDDocument()) {
            d1.addPage(new PDPage(PDRectangle.A4));
            d1.addPage(new PDPage(PDRectangle.A4));
            d1.save(pdf1.toFile());
        }
        try (PDDocument d2 = new PDDocument()) {
            d2.addPage(new PDPage(PDRectangle.A4));
            d2.save(pdf2.toFile());
        }
        PageSequence seq = PdfPageLoader.loadAll(List.of(pdf1, pdf2));
        assertEquals(3, seq.pageCount());
        assertEquals(pdf1.toString(), seq.getPages().get(0).getSourceDocumentId().orElseThrow());
        assertEquals(pdf1.toString(), seq.getPages().get(1).getSourceDocumentId().orElseThrow());
        assertEquals(pdf2.toString(), seq.getPages().get(2).getSourceDocumentId().orElseThrow());
        assertEquals(0, seq.getPages().get(0).getSourcePageIndex().orElseThrow());
        assertEquals(1, seq.getPages().get(1).getSourcePageIndex().orElseThrow());
        assertEquals(0, seq.getPages().get(2).getSourcePageIndex().orElseThrow());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, seq.getPages().get(i).getPhysicalPosition());
        }
    }

    @Test
    void loadAllNullEntryThrows() {
        List<Path> paths = new ArrayList<>();
        paths.add(null);
        assertThrows(NullPointerException.class, () -> PdfPageLoader.loadAll(paths));
    }
}
