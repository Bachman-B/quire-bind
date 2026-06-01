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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Pre-process: strip non-renderable elements and inject layout fixes
        String cleaned = preprocess(html);
        String htmlWithPage = injectPageSize(cleaned, paperSize);

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

    /**
     * Strips elements and rules that iText 7 cannot render:
     * script blocks, canvas elements, and CDN font @import rules.
     */
    private static String preprocess(String html) {
        // Remove <script>...</script>
        html = html.replaceAll("(?si)<script[^>]*>.*?</script>", "");
        // Remove <canvas> elements
        html = html.replaceAll("(?si)<canvas[^>]*>.*?</canvas>", "");
        html = html.replaceAll("(?i)<canvas[^>]*/?>", "");
        // Remove CDN @import rules (fonts can't be fetched at conversion time)
        html = html.replaceAll("@import\\s+url\\([^)]+\\)[^;]*;?", "");
        // Wrap every SVG in a block div so iText 7 treats it as a block element,
        // and add explicit dimensions so it scales to page width rather than
        // expanding the page to its natural viewBox size.
        html = html.replaceAll("(?i)(<svg)\\b",
            "<div style=\"width:100%;overflow:hidden;\"><svg style=\"width:100%;height:auto;\"");
        html = html.replaceAll("(?i)(</svg>)", "$1</div>");
        return html;
    }

    /**
     * Detects the largest fixed-size CSS container in {@code <style>} blocks only
     * (e.g. a full-page cover div) and uses its dimensions as the PDF page size.
     * Falls back to the requested paper size when no fixed container is detected.
     * Searching only inside {@code <style>} tags prevents body content (such as
     * code blocks with CSS examples) from triggering a false detection.
     */
    private static String injectPageSize(String html, PaperSize paperSize) {
        // Extract only CSS inside <style> tags to avoid matching body content
        StringBuilder cssOnly = new StringBuilder();
        Pattern styleTag = Pattern.compile("(?si)<style[^>]*>(.*?)</style>");
        Matcher styleM = styleTag.matcher(html);
        while (styleM.find()) {
            cssOnly.append(styleM.group(1)).append('\n');
        }

        Pattern dim = Pattern.compile("width\\s*:\\s*(\\d+)px[^}]*?height\\s*:\\s*(\\d+)px",
            Pattern.DOTALL);
        Matcher m = dim.matcher(cssOnly);
        float pageMm0 = 0;
        float pageMm1 = 0;
        int maxArea = 0;
        while (m.find()) {
            int pw = Integer.parseInt(m.group(1));
            int ph = Integer.parseInt(m.group(2));
            int area = pw * ph;
            // Only consider containers that look like full pages (> 400×400 px)
            if (area > maxArea && pw > 400 && ph > 400) {
                maxArea = area;
                pageMm0 = pw / 96f * 25.4f;   // px → mm at 96 DPI
                pageMm1 = ph / 96f * 25.4f;
            }
        }

        String pageRule;
        String bodyRule = "html,body{margin:0!important;padding:0!important;"
            + "display:block!important;min-height:0!important;}"
            // Constrain media so they never overflow the page and break pagination
            + "svg,img,figure{max-width:100%!important;width:auto!important;"
            + "height:auto!important;display:block;}";
        if (pageMm0 > 0) {
            // Use the detected container size with zero margin so content fits exactly
            pageRule = String.format("@page{size:%.1fmm %.1fmm;margin:0;}", pageMm0, pageMm1);
        } else {
            float[] dims = pageDimsMm(paperSize);
            pageRule = String.format("@page{size:%.1fmm %.1fmm;margin:10mm;}", dims[0], dims[1]);
        }

        String inject = "<style>" + bodyRule + pageRule + "</style>";
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
