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
package com.maiitsoh.quirebind.core.creep;

import com.maiitsoh.quirebind.core.model.CreepConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class CreepTransformApplierTest {

    @Test
    void outermostSheetHasZeroShift() {
        assertEquals(0f, CreepTransformApplier.shiftPt(0, 0.1), 0.001f);
    }

    @Test
    void shiftIncreasesWithSheetIndex() {
        float shift1 = CreepTransformApplier.shiftPt(1, 0.1);
        float shift2 = CreepTransformApplier.shiftPt(2, 0.1);
        assertTrue(shift1 > 0f);
        assertTrue(shift2 > shift1);
    }

    @Test
    void shiftFormula() {
        // sheetIndex=1, thickness=0.1 mm → shiftMm = 1×2×0.1 = 0.2 mm → pt = 0.2×72/25.4
        float expected = (float) (0.2 * 72.0 / 25.4);
        assertEquals(expected, CreepTransformApplier.shiftPt(1, 0.1), 0.001f);
    }

    @Test
    void zeroThicknessGivesZeroShift() {
        assertEquals(0f, CreepTransformApplier.shiftPt(5, 0.0), 0.001f);
    }

    @Test
    void configOverloadUsesThickness() {
        CreepConfig config = CreepConfig.builder().paperThicknessMm(0.1).build();
        float direct = CreepTransformApplier.shiftPt(2, 0.1);
        float fromConfig = CreepTransformApplier.shiftPt(2, config);
        assertEquals(direct, fromConfig, 0.001f);
    }

    @Test
    void configOverloadDerivesFromGsm() {
        // 80 gsm × 0.00125 = 0.1 mm
        CreepConfig config = CreepConfig.builder().paperWeightGsm(80.0).build();
        float fromGsm = CreepTransformApplier.shiftPt(1, config);
        float fromMm = CreepTransformApplier.shiftPt(1, 0.1);
        assertEquals(fromMm, fromGsm, 0.001f);
    }

    @Test
    void configOverloadReturnsZeroWhenNeitherSet() {
        CreepConfig config = CreepConfig.builder().build();
        assertEquals(0f, CreepTransformApplier.shiftPt(3, config), 0.001f);
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<CreepTransformApplier> ctor =
                CreepTransformApplier.class.getDeclaredConstructor();
        assertNotNull(ctor);
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
