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
 * Converts a Markdown file (.md / .markdown) to a PDF via HTML.
 *
 * <p>Markdown is parsed using flexmark-java (CommonMark compatible), wrapped in a
 * minimal HTML page with page-size and margin CSS, then converted to PDF by
 * {@link HtmlToPdfConverter}.
 */
public final class MarkdownToPdfConverter {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

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
        return convertMarkdown(markdown, mdFile.getFileName().toString(), paperSize);
    }

    /**
     * Converts a Markdown string to a PDF written to a new temp file.
     *
     * @param markdown  the Markdown source; must not be null
     * @param title     document title used in the HTML {@code <title>} element
     * @param paperSize target page size; must not be null
     * @return path to the generated PDF temp file
     * @throws IOException if writing the PDF fails
     */
    public static Path convertMarkdown(String markdown, String title, PaperSize paperSize)
            throws IOException {
        Objects.requireNonNull(markdown, "markdown");
        Objects.requireNonNull(paperSize, "paperSize");
        Document doc = PARSER.parse(markdown);
        String body = RENDERER.render(doc);
        float[] dims = HtmlToPdfConverter.pageDimsMm(paperSize);
        String html = buildHtmlPage(title != null ? title : "Document", body, dims);
        return HtmlToPdfConverter.convertHtml(html, null, paperSize);
    }

    private static String buildHtmlPage(String title, String body, float[] dims) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
            + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
            + "  <title>" + escapeXml(title) + "</title>\n"
            + "  <style type=\"text/css\">\n"
            + "    @page { size: " + dims[0] + "mm " + dims[1] + "mm; margin: 20mm; }\n"
            + "    body  { font-family: Georgia, serif; font-size: 11pt; line-height: 1.5; "
            + "            color: #000; }\n"
            + "    h1    { font-size: 20pt; margin: 0 0 12pt; }\n"
            + "    h2    { font-size: 15pt; margin: 14pt 0 8pt; }\n"
            + "    h3    { font-size: 12pt; margin: 10pt 0 6pt; }\n"
            + "    p     { margin: 0 0 8pt; }\n"
            + "    pre   { font-family: monospace; font-size: 9pt; background: #f5f5f5;"
            + "            padding: 6pt; white-space: pre-wrap; }\n"
            + "    code  { font-family: monospace; font-size: 9pt; }\n"
            + "    ul, ol { margin: 0 0 8pt 20pt; }\n"
            + "    blockquote { margin: 8pt 0 8pt 20pt; border-left: 3pt solid #ccc;"
            + "                 padding-left: 8pt; color: #555; }\n"
            + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + body + "\n"
            + "</body>\n"
            + "</html>";
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
