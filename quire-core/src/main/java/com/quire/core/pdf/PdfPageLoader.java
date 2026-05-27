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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads a PDF file into a {@link PageSequence} of {@link PageType#CONTENT} pages.
 *
 * <p>Each page in the source PDF becomes one {@link QuirePage} with:
 * <ul>
 *   <li>{@code pageType = CONTENT}</li>
 *   <li>{@code sourceDocumentId = pdfPath.toString()}</li>
 *   <li>{@code sourcePageIndex = 0-based page index in the source PDF}</li>
 *   <li>{@code physicalPosition = 0-based index in the resulting sequence}</li>
 * </ul>
 *
 * <p>The PDF document is opened and closed within this call; no PDFBox object is
 * kept alive after {@link #load(Path)} returns.
 */
public final class PdfPageLoader {

    private PdfPageLoader() {
    }

    /**
     * Loads all pages from the given PDF into a new {@link PageSequence}.
     *
     * @param pdfPath path to the source PDF; must not be null
     * @return a sequence with one CONTENT page per source page
     * @throws IOException if the file cannot be read or is not a valid PDF
     */
    public static PageSequence load(Path pdfPath) throws IOException {
        Objects.requireNonNull(pdfPath, "pdfPath");
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PageSequence seq = new PageSequence();
            String docId = pdfPath.toString();
            int pageCount = doc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                QuirePage page = QuirePage.builder()
                        .physicalPosition(i)
                        .pageType(PageType.CONTENT)
                        .sourceDocumentId(docId)
                        .sourcePageIndex(i)
                        .build();
                seq.insertPage(i, page);
            }
            return seq;
        }
    }
}
