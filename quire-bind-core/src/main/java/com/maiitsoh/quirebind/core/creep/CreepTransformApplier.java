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

/**
 * Calculates the per-sheet content shift for creep compensation.
 *
 * <h3>What is creep?</h3>
 * When sheets are nested inside one another to form a signature, inner sheets protrude
 * further toward the fore-edge than outer sheets. If the book block is trimmed to a
 * uniform fore-edge, inner pages end up with less usable margin than outer pages.
 *
 * <h3>Compensation strategy</h3>
 * Before trimming, each sheet's content is shifted inward (toward the spine / fold line)
 * by its creep amount. After trimming, all pages then have consistent margins.
 *
 * <p>For a FOLIO sheet at {@code sheetIndex} (0 = outermost) with paper of thickness
 * {@code t mm}:
 * <pre>creepMm = sheetIndex × 2 × t</pre>
 * The content on the left half of the imposed sheet is shifted <em>right</em> (toward the
 * fold), and content on the right half is shifted <em>left</em> (toward the fold), each
 * by {@code creepMm} converted to PDF points.
 */
public final class CreepTransformApplier {

    private static final double MM_TO_PT = 72.0 / 25.4;

    private CreepTransformApplier() {
    }

    /**
     * Returns the shift in PDF points to apply to page content on a sheet at the given index.
     *
     * <p>The caller applies a positive shift to left-half pages (toward the fold) and a
     * negative shift to right-half pages (toward the fold).
     *
     * @param sheetIndex       0-based sheet index within the signature (0 = outermost, no shift)
     * @param paperThicknessMm paper thickness in millimetres; must be &gt;= 0
     * @return shift in PDF points; 0 for the outermost sheet
     */
    public static float shiftPt(int sheetIndex, double paperThicknessMm) {
        double shiftMm = sheetIndex * 2.0 * paperThicknessMm;
        return (float) (shiftMm * MM_TO_PT);
    }

    /**
     * Convenience overload that derives the paper thickness from a {@link CreepConfig}.
     * Uses {@code paperThicknessMm} if present, otherwise approximates from
     * {@code paperWeightGsm} using {@code gsm × 0.00125}. Returns 0 if neither is set.
     *
     * @param sheetIndex 0-based sheet index within the signature
     * @param config     creep configuration supplying paper properties; must not be null
     * @return shift in PDF points
     */
    public static float shiftPt(int sheetIndex, CreepConfig config) {
        double thicknessMm = config.getPaperThicknessMm()
                .orElseGet(() -> config.getPaperWeightGsm()
                        .map(g -> g * CreepCalculator.GSM_TO_MM)
                        .orElse(0.0));
        return shiftPt(sheetIndex, thicknessMm);
    }
}
