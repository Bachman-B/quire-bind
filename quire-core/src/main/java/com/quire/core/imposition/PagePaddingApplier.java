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
import com.quire.core.model.PaddingConfig;
import com.quire.core.model.PageType;
import com.quire.core.model.QuirePage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inserts padding pages around the content pages according to the project's
 * {@link PaddingConfig} and {@link ImpositionGroup}.
 *
 * <p>Padding is applied in this order:
 * <ol>
 *   <li>Aesthetic-front pages prepended to the sequence</li>
 *   <li>Completion-front blanks prepended (Group C only)</li>
 *   <li>Content pages</li>
 *   <li>Completion-rear blanks appended (Group C only)</li>
 *   <li>Aesthetic-rear pages appended to the sequence</li>
 *   <li>Filler blanks appended to bring total to a multiple of 4 (Groups A and B)</li>
 * </ol>
 *
 * <p>Physical positions are reindexed after all pages are assembled.
 */
public final class PagePaddingApplier {

    private PagePaddingApplier() {
    }

    /**
     * Returns a new padded list of pages.
     *
     * @param contentPages source pages; must not be null
     * @param config       padding configuration; must not be null
     * @param group        imposition group; must not be null
     * @return an immutable list of pages in physical order, positions reindexed from 0
     */
    public static List<QuirePage> pad(
            List<QuirePage> contentPages,
            PaddingConfig config,
            ImpositionGroup group) {
        Objects.requireNonNull(contentPages, "contentPages");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(group, "group");

        List<QuirePage> result = new ArrayList<>();

        for (int i = 0; i < config.getAestheticFront(); i++) {
            result.add(blankPage(PageType.AESTHETIC));
        }
        if (group == ImpositionGroup.C) {
            for (int i = 0; i < config.getCompletionFront(); i++) {
                result.add(blankPage(PageType.COMPLETION_BLANK));
            }
        }

        result.addAll(contentPages);

        if (group == ImpositionGroup.C) {
            for (int i = 0; i < config.getCompletionRear(); i++) {
                result.add(blankPage(PageType.COMPLETION_BLANK));
            }
        }
        for (int i = 0; i < config.getAestheticRear(); i++) {
            result.add(blankPage(PageType.AESTHETIC));
        }

        if (group != ImpositionGroup.C) {
            int remainder = result.size() % 4;
            int fillers = (remainder == 0) ? 0 : (4 - remainder);
            for (int i = 0; i < fillers; i++) {
                result.add(blankPage(PageType.FILLER_BLANK));
            }
        }

        return reindex(result);
    }

    private static QuirePage blankPage(PageType type) {
        return QuirePage.builder().physicalPosition(0).pageType(type).build();
    }

    private static List<QuirePage> reindex(List<QuirePage> pages) {
        List<QuirePage> out = new ArrayList<>(pages.size());
        for (int i = 0; i < pages.size(); i++) {
            QuirePage p = pages.get(i);
            out.add(p.getPhysicalPosition() == i ? p : p.toBuilder().physicalPosition(i).build());
        }
        return List.copyOf(out);
    }
}
