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

class SewingConfigTest {

    @Test
    void defaultsHaveExpectedValues() {
        SewingConfig cfg = SewingConfig.defaults();
        assertEquals(5, cfg.getHoleCount());
        assertEquals(15.0, cfg.getEndMarginMm());
    }

    @Test
    void builderSetsAllFields() {
        SewingConfig cfg = SewingConfig.builder()
                .holeCount(7)
                .endMarginMm(12.5)
                .build();
        assertEquals(7, cfg.getHoleCount());
        assertEquals(12.5, cfg.getEndMarginMm());
    }

    @Test
    void holeCountTooLowThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SewingConfig.builder().holeCount(1).build());
    }

    @Test
    void endMarginMmZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SewingConfig.builder().endMarginMm(0).build());
    }

    @Test
    void endMarginMmNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SewingConfig.builder().endMarginMm(-5).build());
    }

    @Test
    void equalsSameValues() {
        SewingConfig a = SewingConfig.builder().holeCount(5).endMarginMm(15.0).build();
        SewingConfig b = SewingConfig.builder().holeCount(5).endMarginMm(15.0).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsDifferentHoleCount() {
        SewingConfig a = SewingConfig.builder().holeCount(3).build();
        SewingConfig b = SewingConfig.builder().holeCount(7).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentEndMargin() {
        SewingConfig a = SewingConfig.builder().endMarginMm(10.0).build();
        SewingConfig b = SewingConfig.builder().endMarginMm(20.0).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsSelf() {
        SewingConfig a = SewingConfig.defaults();
        assertEquals(a, a);
    }

    @Test
    void equalsNull() {
        assertNotEquals(SewingConfig.defaults(), null);
    }

    @Test
    void equalsWrongType() {
        assertNotEquals(SewingConfig.defaults(), "other");
    }

    @Test
    void toStringContainsFields() {
        String s = SewingConfig.builder().holeCount(3).endMarginMm(10.0).build().toString();
        assertTrue(s.contains("holeCount=3"));
        assertTrue(s.contains("endMarginMm=10.0"));
    }
}
