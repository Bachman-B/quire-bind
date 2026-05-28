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

import com.maiitsoh.quirebind.core.model.FolioStyle;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.QuirePage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FolioAssignerTest {

    private static QuirePage page(PageType type) {
        return QuirePage.builder().physicalPosition(0).pageType(type).build();
    }

    private static NumberingConfig defaultConfig() {
        return NumberingConfig.builder().build();
    }

    @Test
    void nullPagesThrows() {
        assertThrows(NullPointerException.class,
                () -> FolioAssigner.assign(null, defaultConfig()));
    }

    @Test
    void nullConfigThrows() {
        assertThrows(NullPointerException.class,
                () -> FolioAssigner.assign(List.of(), null));
    }

    @Test
    void noContentPagesReturnsUnchanged() {
        List<QuirePage> pages = List.of(page(PageType.FILLER_BLANK), page(PageType.AESTHETIC));
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertEquals(pages, result);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
    }

    @Test
    void bodyPagesGetArabicNumbers() {
        List<QuirePage> pages = List.of(
                page(PageType.CONTENT), page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertEquals(1, result.get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(2, result.get(1).getLogicalPageNumber().orElseThrow());
        assertEquals(3, result.get(2).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void bodyStartNumberRespected() {
        NumberingConfig cfg = NumberingConfig.builder().bodyStartNumber(5).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(5, result.get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(6, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void suppressFirstBodyFolioRemovesFirstNumber() {
        NumberingConfig cfg = NumberingConfig.builder().suppressFirstBodyFolio(true).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertEquals(2, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void frontMatterStyleNoneNoFolios() {
        NumberingConfig cfg = NumberingConfig.builder().frontMatterStyle(FolioStyle.NONE).build();
        List<QuirePage> pages = List.of(page(PageType.AESTHETIC), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
    }

    @Test
    void frontMatterStyleArabicGetsFolios() {
        NumberingConfig cfg = NumberingConfig.builder().frontMatterStyle(FolioStyle.ARABIC).build();
        List<QuirePage> pages = List.of(
                page(PageType.AESTHETIC), page(PageType.AESTHETIC), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(1, result.get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(2, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void frontMatterStartNumberRespected() {
        NumberingConfig cfg = NumberingConfig.builder()
                .frontMatterStyle(FolioStyle.ARABIC)
                .frontMatterStartNumber(3)
                .build();
        List<QuirePage> pages = List.of(
                page(PageType.AESTHETIC), page(PageType.AESTHETIC), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(3, result.get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(4, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void frontMatterRomanGetsFolios() {
        NumberingConfig cfg = NumberingConfig.builder().frontMatterStyle(FolioStyle.ROMAN).build();
        List<QuirePage> pages = List.of(page(PageType.AESTHETIC), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(1, result.get(0).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void rearMatterStyleNoneNoFolios() {
        NumberingConfig cfg = NumberingConfig.builder().rearMatterStyle(FolioStyle.NONE).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.AESTHETIC));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
    }

    @Test
    void rearMatterStyleArabicGetsFolios() {
        NumberingConfig cfg = NumberingConfig.builder().rearMatterStyle(FolioStyle.ARABIC).build();
        List<QuirePage> pages = List.of(
                page(PageType.CONTENT), page(PageType.AESTHETIC), page(PageType.AESTHETIC));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(1, result.get(1).getLogicalPageNumber().orElseThrow());
        assertEquals(2, result.get(2).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void rearMatterStartNumberRespected() {
        NumberingConfig cfg = NumberingConfig.builder()
                .rearMatterStyle(FolioStyle.ARABIC)
                .rearMatterStartNumber(5)
                .build();
        List<QuirePage> pages = List.of(
                page(PageType.CONTENT), page(PageType.AESTHETIC), page(PageType.AESTHETIC));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(5, result.get(1).getLogicalPageNumber().orElseThrow());
        assertEquals(6, result.get(2).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void rearMatterRomanGetsFolios() {
        NumberingConfig cfg = NumberingConfig.builder().rearMatterStyle(FolioStyle.ROMAN).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.AESTHETIC));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(1, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void fillerBlankNeverGetsFolio() {
        List<QuirePage> pages = List.of(
                page(PageType.CONTENT), page(PageType.FILLER_BLANK),
                page(PageType.FILLER_BLANK), page(PageType.FILLER_BLANK));
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(2).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(3).getLogicalPageNumber().isEmpty());
    }

    @Test
    void completionBlankNeverGetsFolio() {
        List<QuirePage> pages = List.of(
                page(PageType.COMPLETION_BLANK), page(PageType.CONTENT),
                page(PageType.COMPLETION_BLANK));
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(2).getLogicalPageNumber().isEmpty());
    }

    @Test
    void completionBlankFrontIsNotCountedAsFrontMatter() {
        NumberingConfig cfg = NumberingConfig.builder().frontMatterStyle(FolioStyle.ARABIC).build();
        List<QuirePage> pages = List.of(
                page(PageType.COMPLETION_BLANK), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertEquals(1, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void bodyStyleNoneNoBodyFolios() {
        NumberingConfig cfg = NumberingConfig.builder().bodyStyle(FolioStyle.NONE).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
    }

    @Test
    void bodyStyleRomanGetsFolios() {
        NumberingConfig cfg = NumberingConfig.builder().bodyStyle(FolioStyle.ROMAN).build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertEquals(1, result.get(0).getLogicalPageNumber().orElseThrow());
        assertEquals(2, result.get(1).getLogicalPageNumber().orElseThrow());
    }

    @Test
    void suppressFirstBodyFolioWithNoneStyleDoesNothing() {
        NumberingConfig cfg = NumberingConfig.builder()
                .bodyStyle(FolioStyle.NONE)
                .suppressFirstBodyFolio(true)
                .build();
        List<QuirePage> pages = List.of(page(PageType.CONTENT), page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, cfg);
        assertTrue(result.get(0).getLogicalPageNumber().isEmpty());
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
    }

    @Test
    void physicalPositionsPreserved() {
        List<QuirePage> pages = List.of(
                QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build(),
                QuirePage.builder().physicalPosition(1).pageType(PageType.CONTENT).build());
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertEquals(0, result.get(0).getPhysicalPosition());
        assertEquals(1, result.get(1).getPhysicalPosition());
    }

    @Test
    void resultIsImmutable() {
        List<QuirePage> result = FolioAssigner.assign(List.of(page(PageType.CONTENT)), defaultConfig());
        assertThrows(UnsupportedOperationException.class, () -> result.add(page(PageType.CONTENT)));
    }

    @Test
    void emptyListReturnsEmpty() {
        List<QuirePage> result = FolioAssigner.assign(List.of(), defaultConfig());
        assertTrue(result.isEmpty());
    }

    @Test
    void aestheticPageBetweenContentPagesGetsNoFolio() {
        // AESTHETIC sandwiched between CONTENT pages — not front matter, not body, not rear matter
        List<QuirePage> pages = List.of(
                page(PageType.CONTENT),
                page(PageType.AESTHETIC),
                page(PageType.CONTENT));
        List<QuirePage> result = FolioAssigner.assign(pages, defaultConfig());
        assertTrue(result.get(1).getLogicalPageNumber().isEmpty());
    }
}
