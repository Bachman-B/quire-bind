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
package com.maiitsoh.quirebind.core.imposition;

import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.ImposedSheet;
import com.maiitsoh.quirebind.core.model.CreepConfig;
import com.maiitsoh.quirebind.core.model.ImpositionLayout;
import com.maiitsoh.quirebind.core.model.MarkConfig;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PaddingConfig;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.QuireProject;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.Signature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImpositionEngineTest {

    private static QuirePage content(int pos) {
        return QuirePage.builder().physicalPosition(pos).pageType(PageType.CONTENT).build();
    }

    private static QuireProject.Builder projectBase() {
        return QuireProject.builder()
                .name("Test")
                .bindingTechnique(BindingTechnique.SADDLE_STITCH)
                .paperSize(PaperSize.A4)
                .readingDirection(ReadingDirection.LTR)
                .layout(ImpositionLayout.FOLIO)
                .paddingConfig(PaddingConfig.builder().build())
                .numberingConfig(NumberingConfig.builder().build())
                .markConfig(MarkConfig.builder().build())
                .creepConfig(CreepConfig.builder().build());
    }

    private static PageSequence seqOf(int count) {
        PageSequence seq = new PageSequence();
        for (int i = 0; i < count; i++) {
            seq.insertPage(i, content(i));
        }
        return seq;
    }

    @Test
    void nullProjectThrows() {
        assertThrows(NullPointerException.class, () -> ImpositionEngine.impose(null));
    }

    @Test
    void groupBFourContentPagesOneSig() {
        QuireProject project = projectBase().pageSequence(seqOf(4)).build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(1, sigs.size());
        assertEquals(1, sigs.get(0).getSheets().size());
    }

    @Test
    void groupBFourContentPagesBodyFolios() {
        QuireProject project = projectBase().pageSequence(seqOf(4)).build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(List.of(1, 2, 3, 4), sigs.get(0).getLogicalPageNumbers());
    }

    @Test
    void groupBFolioImpositionLtrCorrect() {
        QuireProject project = projectBase().pageSequence(seqOf(4)).build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        var sheet = sigs.get(0).getSheets().get(0);
        assertEquals(4, sheet.getFrontPages().get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(1, sheet.getFrontPages().get(1).getLogicalPageNumber().orElseThrow());
        assertEquals(2, sheet.getBackPages().get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(3, sheet.getBackPages().get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void groupBFillerAddedForTwoContentPages() {
        PageSequence seq = new PageSequence();
        seq.insertPage(0, content(0));
        seq.insertPage(1, content(1));
        QuireProject project = projectBase().pageSequence(seq).build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(1, sigs.size());
        assertEquals(1, sigs.get(0).getSheets().size());
        assertEquals(4, totalPages(sigs));
    }

    @Test
    void groupBAestheticFrontApplied() {
        PaddingConfig cfg = PaddingConfig.builder().aestheticFront(2).build();
        QuireProject project = projectBase()
                .paddingConfig(cfg)
                .pageSequence(seqOf(2))
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(1, sigs.size());
        assertEquals(1, sigs.get(0).getSheets().size());
    }

    @Test
    void groupBRtlDirection() {
        QuireProject project = projectBase()
                .readingDirection(ReadingDirection.RTL)
                .pageSequence(seqOf(4))
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        var sheet = sigs.get(0).getSheets().get(0);
        assertEquals(1, sheet.getFrontPages().get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(4, sheet.getFrontPages().get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void groupAFourPagesTwoSigs() {
        QuireProject project = projectBase()
                .bindingTechnique(BindingTechnique.PERFECT_BINDING)
                .pageSequence(seqOf(8))
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(2, sigs.size());
    }

    @Test
    void groupAOneContentPageProducesOneSheetWithFillers() {
        // 1 content page is padded to 4 (multiple-of-4 rule for Group A FOLIO layout)
        // → 1 signature, 1 sheet, 2 output PDF pages (front + back of the landscape sheet)
        // The 3 filler blanks fill the remaining positions on the sheet.
        QuireProject project = projectBase()
                .bindingTechnique(BindingTechnique.PERFECT_BINDING)
                .pageSequence(seqOf(1))
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(1, sigs.size());
        assertEquals(1, sigs.get(0).getSheets().size());
        // FOLIO ordering: front=[pages[3], pages[0]], back=[pages[1], pages[2]]
        // pages = [content, filler, filler, filler] so:
        // front=[filler, content], back=[filler, filler]
        ImposedSheet sheet = sigs.get(0).getSheets().get(0);
        assertEquals(PageType.FILLER_BLANK, sheet.getFrontPages().get(0).getPageType());
        assertEquals(PageType.CONTENT,      sheet.getFrontPages().get(1).getPageType());
        assertEquals(PageType.FILLER_BLANK, sheet.getBackPages().get(0).getPageType());
        assertEquals(PageType.FILLER_BLANK, sheet.getBackPages().get(1).getPageType());
    }

    @Test
    void groupCSigSizeTwoSixteenPagesTwoSigs() {
        PaddingConfig cfg = PaddingConfig.builder().signatureSize(2).build();
        QuireProject project = projectBase()
                .bindingTechnique(BindingTechnique.COPTIC)
                .paddingConfig(cfg)
                .pageSequence(seqOf(16))
                .build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertEquals(2, sigs.size());
        assertEquals(2, sigs.get(0).getSheets().size());
    }

    @Test
    void quartoLayoutThrowsUnsupported() {
        QuireProject project = projectBase()
                .layout(ImpositionLayout.QUARTO)
                .pageSequence(seqOf(4))
                .build();
        assertThrows(UnsupportedOperationException.class, () -> ImpositionEngine.impose(project));
    }

    @Test
    void resultIsImmutable() {
        QuireProject project = projectBase().pageSequence(seqOf(4)).build();
        List<Signature> sigs = ImpositionEngine.impose(project);
        assertThrows(UnsupportedOperationException.class,
                () -> sigs.add(Signature.builder().build()));
    }

    private static int totalPages(List<Signature> sigs) {
        return sigs.stream()
                .flatMap(s -> s.getSheets().stream())
                .mapToInt(sh -> sh.getFrontPages().size() + sh.getBackPages().size())
                .sum();
    }
}
