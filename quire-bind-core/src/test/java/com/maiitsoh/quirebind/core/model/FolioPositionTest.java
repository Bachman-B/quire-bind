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

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolioPositionTest {

    @Test
    void valuesExist() {
        assertEquals(6, FolioPosition.values().length);
    }

    @Test
    void valueOfRoundtrips() {
        assertEquals(FolioPosition.TOP_INNER,    FolioPosition.valueOf("TOP_INNER"));
        assertEquals(FolioPosition.TOP_CENTER,   FolioPosition.valueOf("TOP_CENTER"));
        assertEquals(FolioPosition.TOP_OUTER,    FolioPosition.valueOf("TOP_OUTER"));
        assertEquals(FolioPosition.BOTTOM_INNER, FolioPosition.valueOf("BOTTOM_INNER"));
        assertEquals(FolioPosition.BOTTOM_CENTER, FolioPosition.valueOf("BOTTOM_CENTER"));
        assertEquals(FolioPosition.BOTTOM_OUTER, FolioPosition.valueOf("BOTTOM_OUTER"));
    }
}
