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

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.maiitsoh.quirebind.core.model.PaperSize;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Converts an HTML file to a PDF using OpenHTMLtoPDF.
 *
 * <p>The input HTML should be well-formed XHTML. The output PDF page size is derived
 * from the supplied {@link PaperSize}. A minimal default stylesheet is applied if the
 * HTML does not supply its own.
 *
 * <p>The output is written to a caller-supplied temp file path that the caller is
 * responsible for deleting when no longer needed.
 */
public final class HtmlToPdfConverter {

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
        return convertHtml(html, htmlFile.toUri().toString(), paperSize);
    }

    /**
     * Converts an HTML string to a PDF written to a new temp file.
     *
     * @param html       the HTML content; must not be null
     * @param baseUri    base URI used to resolve relative resources; may be null
     * @param paperSize  target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    public static Path convertHtml(String html, String baseUri, PaperSize paperSize)
            throws IOException {
        Objects.requireNonNull(html, "html");
        Objects.requireNonNull(paperSize, "paperSize");
        Path out = Files.createTempFile("quire-convert-", ".pdf");
        try (OutputStream os = Files.newOutputStream(out)) {
            float[] dims = pageDimsMm(paperSize);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useDefaultPageSize(dims[0], dims[1], PdfRendererBuilder.PageSizeUnits.MM);
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();
        } catch (IOException e) {
            Files.deleteIfExists(out);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(out);
            throw new IOException("HTML to PDF conversion failed: " + e.getMessage(), e);
        }
        return out;
    }

    /** Returns {widthMm, heightMm} for the given paper size. */
    static float[] pageDimsMm(PaperSize size) {
        return switch (size) {
            case A3         -> new float[]{297f, 420f};
            case A4         -> new float[]{210f, 297f};
            case A5         -> new float[]{148f, 210f};
            case LETTER     -> new float[]{216f, 279f};
            case LEGAL      -> new float[]{216f, 356f};
            case HALF_LETTER -> new float[]{140f, 216f};
            case CUSTOM     -> new float[]{210f, 297f}; // fall back to A4
        };
    }
}
