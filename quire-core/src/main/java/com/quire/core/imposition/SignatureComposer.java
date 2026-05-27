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

import com.quire.core.model.ImposedSheet;
import com.quire.core.model.ImpositionGroup;
import com.quire.core.model.ImpositionLayout;
import com.quire.core.model.QuirePage;
import com.quire.core.model.ReadingDirection;
import com.quire.core.model.Signature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Folds a flat, numbered page list into {@link Signature} objects using FOLIO imposition.
 *
 * <p>Only {@link ImpositionLayout#FOLIO} is supported in Phase 1. Requesting
 * {@link ImpositionLayout#QUARTO} or {@link ImpositionLayout#OCTAVO} throws
 * {@link UnsupportedOperationException}.
 *
 * <h3>FOLIO imposition formula (0-based sheet index {@code i}, N pages per signature)</h3>
 * <ul>
 *   <li>LTR — front: [pages[N-1-2i], pages[2i]], back: [pages[2i+1], pages[N-2-2i]]</li>
 *   <li>RTL — front: [pages[2i], pages[N-1-2i]], back: [pages[N-2-2i], pages[2i+1]]</li>
 * </ul>
 */
public final class SignatureComposer {

    private SignatureComposer() {
    }

    /**
     * Composes signatures from a numbered, padded page list.
     *
     * @param pages         numbered pages; total count must be divisible by 4 (group A or B) or
     *                      by {@code 4 × signatureSize} (group C); must not be null
     * @param group         imposition group; must not be null
     * @param layout        imposition layout; must not be null; only FOLIO is supported
     * @param signatureSize sheets per signature (ignored for groups A and B); must be positive
     * @param direction     reading direction; must not be null
     * @return an immutable list of {@link Signature} objects
     * @throws UnsupportedOperationException if layout is not {@link ImpositionLayout#FOLIO}
     */
    public static List<Signature> compose(
            List<QuirePage> pages,
            ImpositionGroup group,
            ImpositionLayout layout,
            int signatureSize,
            ReadingDirection direction) {
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(direction, "direction");
        if (layout != ImpositionLayout.FOLIO) {
            throw new UnsupportedOperationException(
                    "Layout " + layout + " is not supported in Phase 1");
        }

        int pagesPerSig = pagesPerSignature(group, signatureSize, pages.size());
        int sigCount = pages.size() / pagesPerSig;
        List<Signature> signatures = new ArrayList<>(sigCount);

        for (int s = 0; s < sigCount; s++) {
            List<QuirePage> sigPages = pages.subList(s * pagesPerSig, (s + 1) * pagesPerSig);
            signatures.add(buildSignature(s, sigPages, direction));
        }

        return List.copyOf(signatures);
    }

    private static int pagesPerSignature(ImpositionGroup group, int signatureSize, int total) {
        return switch (group) {
            case A -> 4;
            case B -> total;
            case C -> signatureSize * 4;
        };
    }

    private static Signature buildSignature(
            int sigIndex, List<QuirePage> sigPages, ReadingDirection dir) {
        int n = sigPages.size();
        int sheetCount = n / 4;
        List<ImposedSheet> sheets = new ArrayList<>(sheetCount);
        List<Integer> logicalNumbers = new ArrayList<>();

        for (QuirePage p : sigPages) {
            p.getLogicalPageNumber().ifPresent(logicalNumbers::add);
        }

        for (int i = 0; i < sheetCount; i++) {
            List<QuirePage> front;
            List<QuirePage> back;
            if (dir == ReadingDirection.LTR) {
                front = List.of(sigPages.get(n - 1 - 2 * i), sigPages.get(2 * i));
                back = List.of(sigPages.get(2 * i + 1), sigPages.get(n - 2 - 2 * i));
            } else {
                front = List.of(sigPages.get(2 * i), sigPages.get(n - 1 - 2 * i));
                back = List.of(sigPages.get(n - 2 - 2 * i), sigPages.get(2 * i + 1));
            }
            sheets.add(ImposedSheet.builder()
                    .sheetIndex(i)
                    .signatureIndex(sigIndex)
                    .frontPages(front)
                    .backPages(back)
                    .build());
        }

        return Signature.builder()
                .signatureIndex(sigIndex)
                .sheets(sheets)
                .logicalPageNumbers(logicalNumbers)
                .build();
    }
}
