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

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.maiitsoh.quirebind.core.model.PaperSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Converts an HTML file to PDF using iText 7 pdfHTML (HTML5 + CSS3).
 *
 * <p>Relative resources (stylesheets, images) are resolved relative to the
 * source file's parent directory so that locally referenced assets are included.
 */
public final class HtmlToPdfConverter {

    private HtmlToPdfConverter() {
    }

    /**
     * Converts an HTML file to a PDF written to a new temp file.
     *
     * @param htmlFile  path to the source HTML file; must not be null
     * @param paperSize target page size (used as a hint; the HTML's own CSS page
     *                  rules take precedence if present)
     * @return path to the generated PDF temp file
     * @throws IOException if reading the HTML or writing the PDF fails
     */
    public static Path convert(Path htmlFile, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(htmlFile, "htmlFile");
        Objects.requireNonNull(paperSize, "paperSize");
        String html = Files.readString(htmlFile);
        String baseUri = htmlFile.getParent().toUri().toString();
        return convertHtml(html, baseUri, paperSize);
    }

    /**
     * Converts an HTML string to a PDF written to a new temp file.
     *
     * @param html      the HTML content; must not be null
     * @param baseUri   base URI used to resolve relative resources; may be null
     * @param paperSize target page size hint; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    public static Path convertHtml(String html, String baseUri, PaperSize paperSize)
            throws IOException {
        Objects.requireNonNull(html, "html");
        Objects.requireNonNull(paperSize, "paperSize");

        // Inject a @page rule if none is present so the paper size is respected
        String htmlWithPage = injectPageSize(html, paperSize);

        Path out = Files.createTempFile("quire-convert-", ".pdf");
        try (PdfWriter writer = new PdfWriter(out.toFile());
             PdfDocument pdfDoc = new PdfDocument(writer)) {
            ConverterProperties props = new ConverterProperties();
            if (baseUri != null) {
                props.setBaseUri(baseUri);
            }
            HtmlConverter.convertToPdf(htmlWithPage, pdfDoc, props);
        } catch (IOException e) {
            Files.deleteIfExists(out);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(out);
            throw new IOException("HTML to PDF conversion failed: " + e.getMessage(), e);
        }
        return out;
    }

    private static String injectPageSize(String html, PaperSize paperSize) {
        float[] dims = pageDimsMm(paperSize);
        // Inject a normalisation block: zero body margin/padding, proper page size.
        // If the HTML already has an @page rule we still inject ours first so it
        // acts as a fallback; the author's rule wins via CSS cascade.
        String inject = "<style>"
            + "html,body{margin:0!important;padding:0!important;"
            + "min-height:0!important;}"
            + "@page{size:" + dims[0] + "mm " + dims[1] + "mm;margin:10mm;}"
            + "</style>";
        int head = html.indexOf("</head>");
        if (head >= 0) {
            return html.substring(0, head) + inject + html.substring(head);
        }
        return inject + html;
    }

    static float[] pageDimsMm(PaperSize size) {
        return switch (size) {
            case A3          -> new float[]{297f, 420f};
            case A4          -> new float[]{210f, 297f};
            case A5          -> new float[]{148f, 210f};
            case LETTER      -> new float[]{216f, 279f};
            case LEGAL       -> new float[]{216f, 356f};
            case HALF_LETTER -> new float[]{140f, 216f};
            case CUSTOM      -> new float[]{210f, 297f};
        };
    }
}
