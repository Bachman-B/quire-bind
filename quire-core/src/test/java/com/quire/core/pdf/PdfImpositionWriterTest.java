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

import com.quire.core.imposition.ImpositionEngine;
import com.quire.core.model.BindingTechnique;
import com.quire.core.model.CreepConfig;
import com.quire.core.model.ImpositionLayout;
import com.quire.core.model.MarkConfig;
import com.quire.core.model.NumberingConfig;
import com.quire.core.model.PaddingConfig;
import com.quire.core.model.PaperSize;
import com.quire.core.model.PageSequence;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;
import com.quire.core.model.QuireProject;
import com.quire.core.model.ReadingDirection;
import com.quire.core.model.Signature;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfImpositionWriterTest {

    @TempDir
    Path tempDir;

    private Path createSourcePdf(int pageCount) throws IOException {
        Path out = tempDir.resolve("source.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.save(out.toFile());
        }
        return out;
    }

    private List<Signature> imposeFourContentPages(Path sourcePdf) throws IOException {
        PageSequence seq = PdfPageLoader.load(sourcePdf);
        QuireProject project = QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .pageSequence(seq)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build())
                .build();
        return ImpositionEngine.impose(project);
    }

    @Test
    void nullSignaturesThrows() {
        assertThrows(NullPointerException.class, () ->
                PdfImpositionWriter.write(null, null,
                        tempDir.resolve("out.pdf"), PaperSize.A4));
    }

    @Test
    void nullOutputPathThrows() {
        assertThrows(NullPointerException.class, () ->
                PdfImpositionWriter.write(List.of(), null, null, PaperSize.A4));
    }

    @Test
    void nullPaperSizeThrows() {
        assertThrows(NullPointerException.class, () ->
                PdfImpositionWriter.write(List.of(), null,
                        tempDir.resolve("out.pdf"), null));
    }

    @Test
    void customPaperSizeThrows() throws IOException {
        Path src = createSourcePdf(4);
        List<Signature> sigs = imposeFourContentPages(src);
        assertThrows(UnsupportedOperationException.class, () ->
                PdfImpositionWriter.write(sigs, src,
                        tempDir.resolve("out.pdf"), PaperSize.CUSTOM));
    }

    @Test
    void fourPageGroupBProducesTwoOutputPages() throws IOException {
        Path src = createSourcePdf(4);
        Path out = tempDir.resolve("out.pdf");
        List<Signature> sigs = imposeFourContentPages(src);
        PdfImpositionWriter.write(sigs, src, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void eightPageGroupBProducesFourOutputPages() throws IOException {
        Path src = createSourcePdf(8);
        Path out = tempDir.resolve("out.pdf");
        PageSequence seq = PdfPageLoader.load(src);
        QuireProject project = QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .pageSequence(seq)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build())
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        PdfImpositionWriter.write(sigs, src, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(4, doc.getNumberOfPages());
        }
    }

    @Test
    void outputPageIsTwiceBookPageWidth() throws IOException {
        Path src = createSourcePdf(4);
        Path out = tempDir.resolve("out.pdf");
        List<Signature> sigs = imposeFourContentPages(src);
        PdfImpositionWriter.write(sigs, src, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            PDRectangle mediaBox = doc.getPage(0).getMediaBox();
            assertEquals(PDRectangle.A4.getWidth() * 2, mediaBox.getWidth(), 0.5f);
            assertEquals(PDRectangle.A4.getHeight(), mediaBox.getHeight(), 0.5f);
        }
    }

    @Test
    void nullSourcePdfWritesBlankPages() throws IOException {
        Path src = createSourcePdf(4);
        Path out = tempDir.resolve("out.pdf");
        List<Signature> sigs = imposeFourContentPages(src);
        PdfImpositionWriter.write(sigs, null, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void aestheticPagesWithSourceDocProduceBlankHalves() throws IOException {
        // srcDoc != null but pages are AESTHETIC → non-CONTENT branch in placePageOnSheet
        Path src = createSourcePdf(4);
        PageSequence seq = new PageSequence();
        for (int i = 0; i < 4; i++) {
            seq.insertPage(i, QuirePage.builder().physicalPosition(i)
                    .pageType(PageType.AESTHETIC).build());
        }
        QuireProject project = QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .pageSequence(seq)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build())
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        Path out = tempDir.resolve("out_aesthetic_src.pdf");
        PdfImpositionWriter.write(sigs, src, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void aestheticPagesProduceBlankHalves() throws IOException {
        PageSequence seq = new PageSequence();
        for (int i = 0; i < 4; i++) {
            seq.insertPage(i, QuirePage.builder().physicalPosition(i)
                    .pageType(PageType.AESTHETIC).build());
        }
        QuireProject project = QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .pageSequence(seq)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build())
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        Path out = tempDir.resolve("out.pdf");
        // aesthetic pages have no sourcePageIndex → blank halves
        PdfImpositionWriter.write(sigs, null, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void contentPageWithoutSourcePageIndexIsBlank() throws IOException {
        // CONTENT pages with no sourcePageIndex → treated as blank even when srcDoc is present
        Path src = createSourcePdf(4);
        PageSequence seq = new PageSequence();
        for (int i = 0; i < 4; i++) {
            seq.insertPage(i, QuirePage.builder().physicalPosition(i)
                    .pageType(PageType.CONTENT).build()); // no sourcePageIndex
        }
        QuireProject project = QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .pageSequence(seq)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build())
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        Path out = tempDir.resolve("out.pdf");
        PdfImpositionWriter.write(sigs, src, out, PaperSize.A4);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void allPaperSizesProduceValidOutput() throws IOException {
        Path src = createSourcePdf(4);
        List<Signature> sigs = imposeFourContentPages(src);
        for (PaperSize size : PaperSize.values()) {
            if (size == PaperSize.CUSTOM) {
                continue;
            }
            Path out = tempDir.resolve("out_" + size + ".pdf");
            PdfImpositionWriter.write(sigs, src, out, size);
            try (PDDocument doc = Loader.loadPDF(out.toFile())) {
                assertEquals(2, doc.getNumberOfPages());
            }
        }
    }
}
