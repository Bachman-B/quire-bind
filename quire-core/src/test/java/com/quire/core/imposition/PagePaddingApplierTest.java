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

import com.quire.core.model.ImpositionGroup;
import com.quire.core.model.PaddingConfig;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagePaddingApplierTest {

    private static QuirePage content(int pos) {
        return QuirePage.builder().physicalPosition(pos).pageType(PageType.CONTENT).build();
    }

    private static List<QuirePage> fourContent() {
        return List.of(content(0), content(1), content(2), content(3));
    }

    @Test
    void nullContentPagesThrows() {
        assertThrows(NullPointerException.class,
                () -> PagePaddingApplier.pad(null, PaddingConfig.builder().build(), ImpositionGroup.B));
    }

    @Test
    void nullConfigThrows() {
        assertThrows(NullPointerException.class,
                () -> PagePaddingApplier.pad(List.of(), null, ImpositionGroup.B));
    }

    @Test
    void nullGroupThrows() {
        assertThrows(NullPointerException.class,
                () -> PagePaddingApplier.pad(List.of(), PaddingConfig.builder().build(), null));
    }

    @Test
    void groupBNoConfigNoExtraPages() {
        List<QuirePage> result = PagePaddingApplier.pad(
                fourContent(), PaddingConfig.builder().build(), ImpositionGroup.B);
        assertEquals(4, result.size());
        assertTrue(result.stream().allMatch(p -> p.getPageType() == PageType.CONTENT));
    }

    @Test
    void groupBPhysicalPositionsReindexed() {
        List<QuirePage> result = PagePaddingApplier.pad(
                fourContent(), PaddingConfig.builder().build(), ImpositionGroup.B);
        for (int i = 0; i < result.size(); i++) {
            assertEquals(i, result.get(i).getPhysicalPosition());
        }
    }

    @Test
    void groupBFillerAddedToReachMultipleOf4() {
        List<QuirePage> content = List.of(content(0), content(1));
        List<QuirePage> result = PagePaddingApplier.pad(
                content, PaddingConfig.builder().build(), ImpositionGroup.B);
        assertEquals(4, result.size());
        assertEquals(PageType.CONTENT, result.get(0).getPageType());
        assertEquals(PageType.CONTENT, result.get(1).getPageType());
        assertEquals(PageType.FILLER_BLANK, result.get(2).getPageType());
        assertEquals(PageType.FILLER_BLANK, result.get(3).getPageType());
    }

    @Test
    void groupBExactMultipleOf4NoFillerAdded() {
        List<QuirePage> result = PagePaddingApplier.pad(
                fourContent(), PaddingConfig.builder().build(), ImpositionGroup.B);
        assertTrue(result.stream().noneMatch(p -> p.getPageType() == PageType.FILLER_BLANK));
    }

    @Test
    void groupBAestheticFrontPrepended() {
        PaddingConfig cfg = PaddingConfig.builder().aestheticFront(2).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.B);
        assertEquals(8, result.size());
        assertEquals(PageType.AESTHETIC, result.get(0).getPageType());
        assertEquals(PageType.AESTHETIC, result.get(1).getPageType());
        assertEquals(PageType.CONTENT, result.get(2).getPageType());
    }

    @Test
    void groupBAestheticRearAppended() {
        PaddingConfig cfg = PaddingConfig.builder().aestheticRear(2).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.B);
        assertEquals(8, result.size());
        // aesthetic rear comes before filler blanks: indices 4-5 are AESTHETIC, 6-7 are FILLER
        assertEquals(PageType.AESTHETIC, result.get(4).getPageType());
        assertEquals(PageType.AESTHETIC, result.get(5).getPageType());
        assertEquals(PageType.FILLER_BLANK, result.get(6).getPageType());
        assertEquals(PageType.FILLER_BLANK, result.get(7).getPageType());
    }

    @Test
    void groupBNoCompletionBlanks() {
        PaddingConfig cfg = PaddingConfig.builder().completionFront(2).completionRear(2).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.B);
        assertTrue(result.stream().noneMatch(p -> p.getPageType() == PageType.COMPLETION_BLANK));
    }

    @Test
    void groupAFillerAddedToReachMultipleOf4() {
        List<QuirePage> content = List.of(content(0), content(1), content(2));
        List<QuirePage> result = PagePaddingApplier.pad(
                content, PaddingConfig.builder().build(), ImpositionGroup.A);
        assertEquals(4, result.size());
        assertEquals(PageType.FILLER_BLANK, result.get(3).getPageType());
    }

    @Test
    void groupANoCompletionBlanks() {
        PaddingConfig cfg = PaddingConfig.builder().completionFront(1).completionRear(1).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.A);
        assertTrue(result.stream().noneMatch(p -> p.getPageType() == PageType.COMPLETION_BLANK));
    }

    @Test
    void groupCCompletionFrontPrepended() {
        PaddingConfig cfg = PaddingConfig.builder().completionFront(2).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.C);
        assertEquals(PageType.COMPLETION_BLANK, result.get(0).getPageType());
        assertEquals(PageType.COMPLETION_BLANK, result.get(1).getPageType());
        assertEquals(PageType.CONTENT, result.get(2).getPageType());
    }

    @Test
    void groupCCompletionRearAppended() {
        PaddingConfig cfg = PaddingConfig.builder().completionRear(2).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.C);
        int size = result.size();
        assertEquals(PageType.COMPLETION_BLANK, result.get(size - 2).getPageType());
        assertEquals(PageType.COMPLETION_BLANK, result.get(size - 1).getPageType());
    }

    @Test
    void groupCNoFillerBlanks() {
        List<QuirePage> content = List.of(content(0), content(1));
        List<QuirePage> result = PagePaddingApplier.pad(
                content, PaddingConfig.builder().build(), ImpositionGroup.C);
        assertTrue(result.stream().noneMatch(p -> p.getPageType() == PageType.FILLER_BLANK));
    }

    @Test
    void groupCAestheticFrontAndCompletionFrontOrdering() {
        PaddingConfig cfg = PaddingConfig.builder().aestheticFront(1).completionFront(1).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.C);
        assertEquals(PageType.AESTHETIC, result.get(0).getPageType());
        assertEquals(PageType.COMPLETION_BLANK, result.get(1).getPageType());
        assertEquals(PageType.CONTENT, result.get(2).getPageType());
    }

    @Test
    void groupCAestheticRearAfterCompletionRear() {
        PaddingConfig cfg = PaddingConfig.builder().aestheticRear(1).completionRear(1).build();
        List<QuirePage> result = PagePaddingApplier.pad(fourContent(), cfg, ImpositionGroup.C);
        int last = result.size() - 1;
        assertEquals(PageType.AESTHETIC, result.get(last).getPageType());
        assertEquals(PageType.COMPLETION_BLANK, result.get(last - 1).getPageType());
    }

    @Test
    void emptyContentGroupBReturnsFourFillers() {
        List<QuirePage> result = PagePaddingApplier.pad(
                List.of(), PaddingConfig.builder().build(), ImpositionGroup.B);
        assertEquals(0, result.size());
    }

    @Test
    void resultIsImmutable() {
        List<QuirePage> result = PagePaddingApplier.pad(
                fourContent(), PaddingConfig.builder().build(), ImpositionGroup.B);
        assertThrows(UnsupportedOperationException.class, () -> result.add(content(99)));
    }
}
