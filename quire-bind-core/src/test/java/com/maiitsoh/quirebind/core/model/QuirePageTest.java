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
package com.maiitsoh.quirebind.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuirePageTest {

    @Test
    void builderRequiresPageType() {
        assertThrows(NullPointerException.class,
                () -> QuirePage.builder().build());
    }

    @Test
    void builderWithMinimalFields() {
        QuirePage page = QuirePage.builder()
                .pageType(PageType.CONTENT)
                .build();
        assertEquals(0, page.getPhysicalPosition());
        assertEquals(PageType.CONTENT, page.getPageType());
        assertTrue(page.getPageZone().isEmpty());
        assertTrue(page.getLogicalPageNumber().isEmpty());
        assertTrue(page.getSourceDocumentId().isEmpty());
        assertTrue(page.getSourcePageIndex().isEmpty());
    }

    @Test
    void builderSetsAllFields() {
        QuirePage page = QuirePage.builder()
                .physicalPosition(3)
                .pageType(PageType.AESTHETIC)
                .pageZone(PageZone.FRONT_MATTER)
                .logicalPageNumber(7)
                .sourceDocumentId("input.pdf")
                .sourcePageIndex(6)
                .build();
        assertEquals(3, page.getPhysicalPosition());
        assertEquals(PageType.AESTHETIC, page.getPageType());
        assertEquals(PageZone.FRONT_MATTER, page.getPageZone().orElseThrow());
        assertEquals(7, page.getLogicalPageNumber().orElseThrow());
        assertEquals("input.pdf", page.getSourceDocumentId().orElseThrow());
        assertEquals(6, page.getSourcePageIndex().orElseThrow());
    }

    @Test
    void negativePhysicalPositionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> QuirePage.builder().physicalPosition(-1));
    }

    @Test
    void logicalPageNumberZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> QuirePage.builder().logicalPageNumber(0));
    }

    @Test
    void logicalPageNumberNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> QuirePage.builder().logicalPageNumber(-1));
    }

    @Test
    void logicalPageNumberNullClearsOptional() {
        QuirePage page = QuirePage.builder()
                .pageType(PageType.FILLER_BLANK)
                .logicalPageNumber(null)
                .build();
        assertTrue(page.getLogicalPageNumber().isEmpty());
    }

    @Test
    void sourcePageIndexNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> QuirePage.builder().sourcePageIndex(-1));
    }

    @Test
    void sourcePageIndexZeroIsAllowed() {
        QuirePage page = QuirePage.builder()
                .pageType(PageType.CONTENT)
                .sourcePageIndex(0)
                .build();
        assertEquals(0, page.getSourcePageIndex().orElseThrow());
    }

    @Test
    void pageTypeNullThrows() {
        assertThrows(NullPointerException.class,
                () -> QuirePage.builder().pageType(null));
    }

    @Test
    void toBuilderRoundtrip() {
        QuirePage original = QuirePage.builder()
                .physicalPosition(2)
                .pageType(PageType.CONTENT)
                .pageZone(PageZone.BODY)
                .logicalPageNumber(5)
                .sourceDocumentId("doc.pdf")
                .sourcePageIndex(4)
                .build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void toBuilderCanUpdatePhysicalPosition() {
        QuirePage original = QuirePage.builder()
                .physicalPosition(0)
                .pageType(PageType.CONTENT)
                .build();
        QuirePage moved = original.toBuilder().physicalPosition(5).build();
        assertEquals(5, moved.getPhysicalPosition());
        assertEquals(PageType.CONTENT, moved.getPageType());
    }

    @Test
    void equalsSameValues() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsDifferentPhysicalPosition() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(1).pageType(PageType.CONTENT).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsSelf() {
        QuirePage a = QuirePage.builder().pageType(PageType.CONTENT).build();
        assertEquals(a, a);
    }

    @Test
    void equalsNull() {
        assertNotEquals(QuirePage.builder().pageType(PageType.CONTENT).build(), null);
    }

    @Test
    void equalsWrongType() {
        assertNotEquals(QuirePage.builder().pageType(PageType.CONTENT).build(), "other");
    }

    @Test
    void equalsDifferentPageType() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.AESTHETIC).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentLogicalPageNumber() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT)
                .logicalPageNumber(1).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentSourceDocumentId() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT)
                .sourceDocumentId("doc.pdf").build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentSourcePageIndex() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT)
                .sourcePageIndex(1).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentPageZone() {
        QuirePage a = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT)
                .pageZone(PageZone.BODY).build();
        QuirePage b = QuirePage.builder().physicalPosition(0).pageType(PageType.CONTENT)
                .pageZone(PageZone.FRONT_MATTER).build();
        assertNotEquals(a, b);
    }

    @Test
    void pageZoneNullClearsOptional() {
        QuirePage page = QuirePage.builder().pageType(PageType.CONTENT)
                .pageZone(null).build();
        assertTrue(page.getPageZone().isEmpty());
    }

    @Test
    void toStringDoesNotThrow() {
        assertNotNull(QuirePage.builder().pageType(PageType.CONTENT).build().toString());
    }
}
