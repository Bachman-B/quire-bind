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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
        return loadAll(List.of(pdfPath));
    }

    /**
     * Loads pages from multiple PDF files into a single {@link PageSequence}, concatenating
     * them in the order supplied. Each page retains its source document identity so the
     * imposition writer can open the correct file when rendering.
     *
     * @param pdfPaths ordered list of source PDF paths; must not be null or contain null
     * @return a sequence with all pages from all sources in supplied order
     * @throws IOException if any file cannot be read or is not a valid PDF
     */
    public static PageSequence loadAll(List<Path> pdfPaths) throws IOException {
        Objects.requireNonNull(pdfPaths, "pdfPaths");
        PageSequence seq = new PageSequence();
        int position = 0;
        for (Path pdfPath : pdfPaths) {
            Objects.requireNonNull(pdfPath, "pdfPaths contains null");
            try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
                String docId = pdfPath.toString();
                int pageCount = doc.getNumberOfPages();
                for (int i = 0; i < pageCount; i++) {
                    QuirePage page = QuirePage.builder()
                            .physicalPosition(position++)
                            .pageType(PageType.CONTENT)
                            .sourceDocumentId(docId)
                            .sourcePageIndex(i)
                            .build();
                    seq.insertPage(seq.pageCount(), page);
                }
            }
        }
        return seq;
    }
}
