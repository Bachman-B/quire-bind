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
package com.quire.core.creep;

import com.quire.core.model.CreepConfig;
import com.quire.core.model.CreepSheetResult;
import com.quire.core.model.ImposedSheet;
import com.quire.core.model.QuirePage;
import com.quire.core.model.Signature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates per-sheet creep amounts for composed signatures.
 *
 * <h3>Creep formula</h3>
 * For a FOLIO signature with {@code S} sheets, sheet {@code i} (0 = outermost, S-1 = innermost)
 * has its fore-edge displaced outward by:
 * <pre>creepMm = sheetIndex × 2 × paperThicknessMm</pre>
 *
 * <p>Paper thickness is taken directly from {@link CreepConfig#getPaperThicknessMm()}.
 * If absent, it is derived from {@link CreepConfig#getPaperWeightGsm()} using the
 * approximation {@code thicknessMm = gsm × 0.00125} (80 gsm → 0.10 mm).
 *
 * <p>If neither thickness nor weight is set the config is returned unchanged.
 * Available spine margins (Phase 2) are not analysed here; all {@link CreepSheetResult}
 * instances are produced without margin data in Phase 1.
 */
public final class CreepCalculator {

    static final double GSM_TO_MM = 0.00125;

    private CreepCalculator() {
    }

    /**
     * Returns an updated {@link CreepConfig} with {@code sheetResults} and
     * {@code calculatedCreepMm} populated.
     *
     * @param signatures composed signatures; must not be null
     * @param config     existing creep config supplying paper properties; must not be null
     * @return updated config, or the original config if thickness cannot be determined
     */
    public static CreepConfig calculate(List<Signature> signatures, CreepConfig config) {
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(config, "config");

        double thicknessMm;
        if (config.getPaperThicknessMm().isPresent()) {
            thicknessMm = config.getPaperThicknessMm().get();
        } else if (config.getPaperWeightGsm().isPresent()) {
            thicknessMm = config.getPaperWeightGsm().get() * GSM_TO_MM;
        } else {
            return config;
        }

        List<CreepSheetResult> results = new ArrayList<>();
        double maxCreep = 0.0;

        for (Signature sig : signatures) {
            for (ImposedSheet sheet : sig.getSheets()) {
                double creep = sheet.getSheetIndex() * 2.0 * thicknessMm;
                results.add(CreepSheetResult.builder()
                        .sheetIndex(sheet.getSheetIndex())
                        .creepMm(creep)
                        .pageNumbers(collectPageNumbers(sheet))
                        .build());
                if (creep > maxCreep) {
                    maxCreep = creep;
                }
            }
        }

        return config.toBuilder()
                .sheetResults(results)
                .calculatedCreepMm(maxCreep)
                .build();
    }

    private static List<Integer> collectPageNumbers(ImposedSheet sheet) {
        List<Integer> nums = new ArrayList<>();
        for (QuirePage p : sheet.getFrontPages()) {
            p.getLogicalPageNumber().ifPresent(nums::add);
        }
        for (QuirePage p : sheet.getBackPages()) {
            p.getLogicalPageNumber().ifPresent(nums::add);
        }
        return nums;
    }
}
