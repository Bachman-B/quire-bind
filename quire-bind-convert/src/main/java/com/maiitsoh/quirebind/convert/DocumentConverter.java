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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Facade for converting supported source formats to PDF.
 *
 * <p>Supported formats: {@code .pdf} (pass-through), {@code .html}, {@code .htm},
 * {@code .md}, {@code .markdown}.
 *
 * <p>For PDF inputs the original path is returned unchanged. For HTML and Markdown
 * inputs a new temp file is created and returned; the caller is responsible for
 * deleting it when no longer needed.
 */
public final class DocumentConverter {

    private DocumentConverter() {
    }

    /**
     * Converts the given file to PDF if necessary.
     *
     * @param sourceFile path to the source file; must not be null
     * @param paperSize  target page size used when converting; must not be null
     * @return the PDF path — the original file if it is already a PDF, or a new
     *         temp file for HTML / Markdown inputs
     * @throws IOException                   if conversion fails
     * @throws UnsupportedOperationException if the file extension is not supported
     */
    public static Path toPdf(Path sourceFile, PaperSize paperSize) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(paperSize, "paperSize");
        String name = sourceFile.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) {
            return sourceFile;
        }
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return HtmlToPdfConverter.convert(sourceFile, paperSize);
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return MarkdownToPdfConverter.convert(sourceFile, paperSize);
        }
        throw new UnsupportedOperationException(
            "Unsupported source format: " + sourceFile.getFileName()
            + ". Supported: .pdf, .html, .htm, .md, .markdown");
    }

    /**
     * Returns true if the given filename has a supported extension.
     *
     * @param filename the file name (may include path components)
     * @return true if the extension is .pdf, .html, .htm, .md, or .markdown
     */
    public static boolean isSupported(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".html") || lower.endsWith(".htm")
            || lower.endsWith(".md") || lower.endsWith(".markdown");
    }
}
