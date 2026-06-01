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

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.maiitsoh.quirebind.core.model.PaperSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Converts a Markdown file (.md / .markdown) to PDF via HTML.
 *
 * <p>Parses CommonMark using flexmark-java, wraps the result in a clean HTML page
 * with a minimal stylesheet, then renders to PDF via {@link HtmlToPdfConverter}
 * (iText 7 pdfHTML).
 */
public final class MarkdownToPdfConverter {

    private static final Parser PARSER =
        Parser.builder(new MutableDataSet()).build();
    private static final HtmlRenderer RENDERER =
        HtmlRenderer.builder(new MutableDataSet()).build();

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
        String body = RENDERER.render(doc);
        float[] dims = HtmlToPdfConverter.pageDimsMm(paperSize);
        String html = wrapHtml(body, dims);
        return HtmlToPdfConverter.convertHtml(html, null, paperSize);
    }

    private static String wrapHtml(String body, float[] dims) {
        return "<!DOCTYPE html>\n<html>\n<head>\n"
            + "<meta charset=\"UTF-8\"/>\n"
            + "<style>\n"
            + "  @page { size: " + dims[0] + "mm " + dims[1] + "mm; margin: 20mm; }\n"
            + "  body  { font-family: Georgia, serif; font-size: 11pt; "
            + "          line-height: 1.5; color: #000; }\n"
            + "  h1 { font-size: 20pt; } h2 { font-size: 15pt; } "
            + "  h3 { font-size: 12pt; }\n"
            + "  p  { margin: 0 0 8pt; }\n"
            + "  pre, code { font-family: monospace; font-size: 9pt; }\n"
            + "  pre { background: #f5f5f5; padding: 6pt; }\n"
            + "  ul, ol { margin: 0 0 8pt 20pt; }\n"
            + "  blockquote { margin: 8pt 0 8pt 20pt; border-left: 3pt solid #ccc;"
            + "               padding-left: 8pt; color: #555; }\n"
            + "</style>\n</head>\n<body>\n"
            + body + "\n</body>\n</html>";
    }
}
