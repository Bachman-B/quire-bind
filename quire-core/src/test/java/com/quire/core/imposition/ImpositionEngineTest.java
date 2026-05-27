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
package com.quire.core.imposition;

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
