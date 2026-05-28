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

class NumberingConfigTest {

    @Test
    void defaultsAreExpected() {
        NumberingConfig cfg = NumberingConfig.builder().build();
        assertEquals(FolioStyle.NONE, cfg.getFrontMatterStyle());
        assertEquals(FolioStyle.ARABIC, cfg.getBodyStyle());
        assertEquals(FolioStyle.NONE, cfg.getRearMatterStyle());
        assertEquals(1, cfg.getFrontMatterStartNumber());
        assertEquals(1, cfg.getBodyStartNumber());
        assertEquals(1, cfg.getRearMatterStartNumber());
        assertFalse(cfg.isSuppressFirstBodyFolio());
        assertEquals(FolioPosition.BOTTOM_OUTER, cfg.getFolioPosition());
    }

    @Test
    void builderSetsAllFields() {
        NumberingConfig cfg = NumberingConfig.builder()
                .frontMatterStyle(FolioStyle.ROMAN)
                .bodyStyle(FolioStyle.ARABIC)
                .rearMatterStyle(FolioStyle.NONE)
                .frontMatterStartNumber(3)
                .bodyStartNumber(5)
                .rearMatterStartNumber(7)
                .suppressFirstBodyFolio(true)
                .folioPosition(FolioPosition.TOP_INNER)
                .build();
        assertEquals(FolioStyle.ROMAN, cfg.getFrontMatterStyle());
        assertEquals(FolioStyle.ARABIC, cfg.getBodyStyle());
        assertEquals(FolioStyle.NONE, cfg.getRearMatterStyle());
        assertEquals(3, cfg.getFrontMatterStartNumber());
        assertEquals(5, cfg.getBodyStartNumber());
        assertEquals(7, cfg.getRearMatterStartNumber());
        assertTrue(cfg.isSuppressFirstBodyFolio());
        assertEquals(FolioPosition.TOP_INNER, cfg.getFolioPosition());
    }

    @Test
    void bodyStartNumberZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().bodyStartNumber(0));
    }

    @Test
    void bodyStartNumberNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().bodyStartNumber(-1));
    }

    @Test
    void frontMatterStyleNullThrows() {
        assertThrows(NullPointerException.class,
                () -> NumberingConfig.builder().frontMatterStyle(null));
    }

    @Test
    void bodyStyleNullThrows() {
        assertThrows(NullPointerException.class,
                () -> NumberingConfig.builder().bodyStyle(null));
    }

    @Test
    void rearMatterStyleNullThrows() {
        assertThrows(NullPointerException.class,
                () -> NumberingConfig.builder().rearMatterStyle(null));
    }

    @Test
    void folioPositionNullThrows() {
        assertThrows(NullPointerException.class,
                () -> NumberingConfig.builder().folioPosition(null));
    }

    @Test
    void toBuilderRoundtrip() {
        NumberingConfig original = NumberingConfig.builder()
                .frontMatterStyle(FolioStyle.ROMAN)
                .bodyStartNumber(3)
                .suppressFirstBodyFolio(true)
                .build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void equalsSameValues() {
        NumberingConfig a = NumberingConfig.builder().bodyStartNumber(2).build();
        NumberingConfig b = NumberingConfig.builder().bodyStartNumber(2).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsDifferentValues() {
        NumberingConfig a = NumberingConfig.builder().bodyStartNumber(1).build();
        NumberingConfig b = NumberingConfig.builder().bodyStartNumber(2).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsSelf() {
        NumberingConfig a = NumberingConfig.builder().build();
        assertEquals(a, a);
    }

    @Test
    void equalsNull() {
        assertNotEquals(NumberingConfig.builder().build(), null);
    }

    @Test
    void equalsWrongType() {
        assertNotEquals(NumberingConfig.builder().build(), "other");
    }

    @Test
    void equalsDifferentSuppressFirstBodyFolio() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().suppressFirstBodyFolio(true).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentFrontMatterStyle() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().frontMatterStyle(FolioStyle.ROMAN).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentBodyStyle() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().bodyStyle(FolioStyle.ROMAN).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentRearMatterStyle() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().rearMatterStyle(FolioStyle.ROMAN).build();
        assertNotEquals(a, b);
    }

    @Test
    void frontMatterStartNumberZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().frontMatterStartNumber(0));
    }

    @Test
    void frontMatterStartNumberNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().frontMatterStartNumber(-1));
    }

    @Test
    void equalsDifferentFrontMatterStartNumber() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().frontMatterStartNumber(5).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentFolioPosition() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().folioPosition(FolioPosition.TOP_INNER).build();
        assertNotEquals(a, b);
    }

    @Test
    void rearMatterStartNumberZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().rearMatterStartNumber(0));
    }

    @Test
    void rearMatterStartNumberNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberingConfig.builder().rearMatterStartNumber(-1));
    }

    @Test
    void equalsDifferentRearMatterStartNumber() {
        NumberingConfig a = NumberingConfig.builder().build();
        NumberingConfig b = NumberingConfig.builder().rearMatterStartNumber(5).build();
        assertNotEquals(a, b);
    }

    @Test
    void toStringDoesNotThrow() {
        assertNotNull(NumberingConfig.builder().build().toString());
    }
}
