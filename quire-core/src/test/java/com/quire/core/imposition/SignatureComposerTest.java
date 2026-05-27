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
import com.quire.core.model.ImpositionLayout;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;
import com.quire.core.model.ReadingDirection;
import com.quire.core.model.Signature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SignatureComposerTest {

    private static List<QuirePage> numberedPages(int count) {
        List<QuirePage> pages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pages.add(QuirePage.builder()
                    .physicalPosition(i)
                    .pageType(PageType.CONTENT)
                    .logicalPageNumber(i + 1)
                    .build());
        }
        return List.copyOf(pages);
    }

    private static List<QuirePage> unnumberedPages(int count) {
        List<QuirePage> pages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pages.add(QuirePage.builder().physicalPosition(i).pageType(PageType.CONTENT).build());
        }
        return List.copyOf(pages);
    }

    @Test
    void nullPagesThrows() {
        assertThrows(NullPointerException.class,
                () -> SignatureComposer.compose(
                        null, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR));
    }

    @Test
    void nullGroupThrows() {
        assertThrows(NullPointerException.class,
                () -> SignatureComposer.compose(
                        List.of(), null, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR));
    }

    @Test
    void nullLayoutThrows() {
        assertThrows(NullPointerException.class,
                () -> SignatureComposer.compose(
                        List.of(), ImpositionGroup.B, null, 1, ReadingDirection.LTR));
    }

    @Test
    void nullDirectionThrows() {
        assertThrows(NullPointerException.class,
                () -> SignatureComposer.compose(
                        List.of(), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, null));
    }

    @Test
    void quartoLayoutThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> SignatureComposer.compose(
                        numberedPages(4), ImpositionGroup.B,
                        ImpositionLayout.QUARTO, 1, ReadingDirection.LTR));
    }

    @Test
    void octavoLayoutThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> SignatureComposer.compose(
                        numberedPages(4), ImpositionGroup.B,
                        ImpositionLayout.OCTAVO, 1, ReadingDirection.LTR));
    }

    // --- Group B (single signature) ---

    @Test
    void groupBFourPagesOneSig() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(4), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(1, sigs.size());
        assertEquals(0, sigs.get(0).getSignatureIndex());
        assertEquals(1, sigs.get(0).getSheets().size());
    }

    @Test
    void groupBEightPagesTwoSheets() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(1, sigs.size());
        assertEquals(2, sigs.get(0).getSheets().size());
    }

    @Test
    void groupBSheetIndices() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(0, sigs.get(0).getSheets().get(0).getSheetIndex());
        assertEquals(1, sigs.get(0).getSheets().get(1).getSheetIndex());
    }

    @Test
    void groupBFolioImpositionLtrFourPages() {
        // pages: 1,2,3,4 (indices 0,1,2,3)
        // sheet 0 (i=0): front=[pages[3],pages[0]]=[4,1], back=[pages[1],pages[2]]=[2,3]
        List<QuirePage> pages = numberedPages(4);
        List<Signature> sigs = SignatureComposer.compose(
                pages, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        var sheet = sigs.get(0).getSheets().get(0);
        assertEquals(pages.get(3), sheet.getFrontPages().get(0));
        assertEquals(pages.get(0), sheet.getFrontPages().get(1));
        assertEquals(pages.get(1), sheet.getBackPages().get(0));
        assertEquals(pages.get(2), sheet.getBackPages().get(1));
    }

    @Test
    void groupBFolioImpositionLtrEightPages() {
        // pages: 0..7
        // sheet 0 (i=0): front=[pages[7],pages[0]], back=[pages[1],pages[6]]
        // sheet 1 (i=1): front=[pages[5],pages[2]], back=[pages[3],pages[4]]
        List<QuirePage> pages = numberedPages(8);
        List<Signature> sigs = SignatureComposer.compose(
                pages, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        var sheet0 = sigs.get(0).getSheets().get(0);
        assertEquals(pages.get(7), sheet0.getFrontPages().get(0));
        assertEquals(pages.get(0), sheet0.getFrontPages().get(1));
        assertEquals(pages.get(1), sheet0.getBackPages().get(0));
        assertEquals(pages.get(6), sheet0.getBackPages().get(1));

        var sheet1 = sigs.get(0).getSheets().get(1);
        assertEquals(pages.get(5), sheet1.getFrontPages().get(0));
        assertEquals(pages.get(2), sheet1.getFrontPages().get(1));
        assertEquals(pages.get(3), sheet1.getBackPages().get(0));
        assertEquals(pages.get(4), sheet1.getBackPages().get(1));
    }

    @Test
    void groupBFolioImpositionRtlFourPages() {
        // RTL sheet 0 (i=0): front=[pages[0],pages[3]], back=[pages[2],pages[1]]
        List<QuirePage> pages = numberedPages(4);
        List<Signature> sigs = SignatureComposer.compose(
                pages, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.RTL);
        var sheet = sigs.get(0).getSheets().get(0);
        assertEquals(pages.get(0), sheet.getFrontPages().get(0));
        assertEquals(pages.get(3), sheet.getFrontPages().get(1));
        assertEquals(pages.get(2), sheet.getBackPages().get(0));
        assertEquals(pages.get(1), sheet.getBackPages().get(1));
    }

    @Test
    void groupBFolioImpositionRtlEightPages() {
        // RTL sheet 0 (i=0): front=[pages[0],pages[7]], back=[pages[6],pages[1]]
        // RTL sheet 1 (i=1): front=[pages[2],pages[5]], back=[pages[4],pages[3]]
        List<QuirePage> pages = numberedPages(8);
        List<Signature> sigs = SignatureComposer.compose(
                pages, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.RTL);
        var sheet0 = sigs.get(0).getSheets().get(0);
        assertEquals(pages.get(0), sheet0.getFrontPages().get(0));
        assertEquals(pages.get(7), sheet0.getFrontPages().get(1));
        assertEquals(pages.get(6), sheet0.getBackPages().get(0));
        assertEquals(pages.get(1), sheet0.getBackPages().get(1));

        var sheet1 = sigs.get(0).getSheets().get(1);
        assertEquals(pages.get(2), sheet1.getFrontPages().get(0));
        assertEquals(pages.get(5), sheet1.getFrontPages().get(1));
        assertEquals(pages.get(4), sheet1.getBackPages().get(0));
        assertEquals(pages.get(3), sheet1.getBackPages().get(1));
    }

    @Test
    void groupBLogicalPageNumbersCollected() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(4), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(List.of(1, 2, 3, 4), sigs.get(0).getLogicalPageNumbers());
    }

    @Test
    void groupBUnnumberedPagesLogicalListEmpty() {
        List<Signature> sigs = SignatureComposer.compose(
                unnumberedPages(4), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertTrue(sigs.get(0).getLogicalPageNumbers().isEmpty());
    }

    // --- Group A (flat, 4 pages per signature) ---

    @Test
    void groupAFourPagesOneSig() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(4), ImpositionGroup.A, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(1, sigs.size());
    }

    @Test
    void groupAEightPagesTwoSigs() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.A, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(2, sigs.size());
        assertEquals(0, sigs.get(0).getSignatureIndex());
        assertEquals(1, sigs.get(1).getSignatureIndex());
    }

    @Test
    void groupAEachSigHasOneSheet() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.A, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(1, sigs.get(0).getSheets().size());
        assertEquals(1, sigs.get(1).getSheets().size());
    }

    @Test
    void groupASecondSigHasCorrectPages() {
        List<QuirePage> pages = numberedPages(8);
        List<Signature> sigs = SignatureComposer.compose(
                pages, ImpositionGroup.A, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        // second sig pages: indices 4,5,6,7
        // sheet i=0: front=[pages[7],pages[4]], back=[pages[5],pages[6]]
        var sheet = sigs.get(1).getSheets().get(0);
        assertEquals(pages.get(7), sheet.getFrontPages().get(0));
        assertEquals(pages.get(4), sheet.getFrontPages().get(1));
        assertEquals(pages.get(5), sheet.getBackPages().get(0));
        assertEquals(pages.get(6), sheet.getBackPages().get(1));
    }

    @Test
    void groupASignatureIndexesAreSequential() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(12), ImpositionGroup.A, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(3, sigs.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, sigs.get(i).getSignatureIndex());
        }
    }

    // --- Group C (multiple signatures of signatureSize*4 pages) ---

    @Test
    void groupCSigSizeOneIsSameAsB() {
        List<QuirePage> pages = numberedPages(4);
        List<Signature> sigsB = SignatureComposer.compose(
                pages, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        List<Signature> sigsC = SignatureComposer.compose(
                pages, ImpositionGroup.C, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(sigsB.size(), sigsC.size());
        assertEquals(sigsB.get(0).getSheets(), sigsC.get(0).getSheets());
    }

    @Test
    void groupCSigSizeTwoEightPagesTwoSheets() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.C, ImpositionLayout.FOLIO, 2, ReadingDirection.LTR);
        assertEquals(1, sigs.size());
        assertEquals(2, sigs.get(0).getSheets().size());
    }

    @Test
    void groupCSigSizeOneTwoSigsOf4() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(8), ImpositionGroup.C, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertEquals(2, sigs.size());
        assertEquals(1, sigs.get(0).getSheets().size());
        assertEquals(1, sigs.get(1).getSheets().size());
    }

    @Test
    void groupCSigSizeTwoSixteenPagesTwoSigs() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(16), ImpositionGroup.C, ImpositionLayout.FOLIO, 2, ReadingDirection.LTR);
        assertEquals(2, sigs.size());
        assertEquals(0, sigs.get(0).getSignatureIndex());
        assertEquals(1, sigs.get(1).getSignatureIndex());
    }

    @Test
    void groupCSheetSignatureIndexMatchesSig() {
        List<Signature> sigs = SignatureComposer.compose(
                numberedPages(16), ImpositionGroup.C, ImpositionLayout.FOLIO, 2, ReadingDirection.LTR);
        for (Signature sig : sigs) {
            for (var sheet : sig.getSheets()) {
                assertEquals(sig.getSignatureIndex(), sheet.getSignatureIndex());
            }
        }
    }

    @Test
    void resultIsImmutable() {
        List<Signature> result = SignatureComposer.compose(
                numberedPages(4), ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR);
        assertThrows(UnsupportedOperationException.class, () -> result.add(
                Signature.builder().build()));
    }
}
