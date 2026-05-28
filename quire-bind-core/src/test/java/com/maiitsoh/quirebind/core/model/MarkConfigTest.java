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

class MarkConfigTest {

    @Test
    void defaultsAreAllFalse() {
        MarkConfig cfg = MarkConfig.builder().build();
        assertFalse(cfg.isFoldLines());
        assertFalse(cfg.isSignatureProofMarkers());
        assertFalse(cfg.isTrimLines());
        assertFalse(cfg.isSewingHoles());
        assertTrue(cfg.getSewingConfig().isEmpty());
    }

    @Test
    void builderSetsAllFields() {
        MarkConfig cfg = MarkConfig.builder()
                .foldLines(true)
                .signatureProofMarkers(true)
                .trimLines(true)
                .sewingHoles(true)
                .build();
        assertTrue(cfg.isFoldLines());
        assertTrue(cfg.isSignatureProofMarkers());
        assertTrue(cfg.isTrimLines());
        assertTrue(cfg.isSewingHoles());
    }

    @Test
    void sewingConfigNullClearsOptional() {
        MarkConfig cfg = MarkConfig.builder().sewingConfig(null).build();
        assertTrue(cfg.getSewingConfig().isEmpty());
    }

    @Test
    void toBuilderRoundtrip() {
        MarkConfig original = MarkConfig.builder()
                .foldLines(true)
                .signatureProofMarkers(true)
                .build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void equalsSameValues() {
        MarkConfig a = MarkConfig.builder().foldLines(true).build();
        MarkConfig b = MarkConfig.builder().foldLines(true).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsDifferentValues() {
        MarkConfig a = MarkConfig.builder().foldLines(true).build();
        MarkConfig b = MarkConfig.builder().foldLines(false).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsSelf() {
        MarkConfig a = MarkConfig.builder().build();
        assertEquals(a, a);
    }

    @Test
    void equalsNull() {
        assertNotEquals(MarkConfig.builder().build(), null);
    }

    @Test
    void equalsWrongType() {
        assertNotEquals(MarkConfig.builder().build(), "other");
    }

    @Test
    void equalsDifferentSignatureProofMarkers() {
        MarkConfig a = MarkConfig.builder().build();
        MarkConfig b = MarkConfig.builder().signatureProofMarkers(true).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentTrimLines() {
        MarkConfig a = MarkConfig.builder().build();
        MarkConfig b = MarkConfig.builder().trimLines(true).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentSewingHoles() {
        MarkConfig a = MarkConfig.builder().build();
        MarkConfig b = MarkConfig.builder().sewingHoles(true).build();
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentSewingConfig() {
        SewingConfig sc = SewingConfig.builder().holeCount(7).build();
        MarkConfig a = MarkConfig.builder().sewingHoles(true).build();
        MarkConfig b = MarkConfig.builder().sewingHoles(true).sewingConfig(sc).build();
        assertNotEquals(a, b);
    }

    @Test
    void toBuilderPreservesSewingConfig() {
        SewingConfig sc = SewingConfig.builder().holeCount(7).endMarginMm(12.0).build();
        MarkConfig original = MarkConfig.builder().sewingConfig(sc).build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void toStringDoesNotThrow() {
        assertNotNull(MarkConfig.builder().build().toString());
    }
}
