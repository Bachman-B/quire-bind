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
package com.maiitsoh.quirebind.core.imposition;

import com.maiitsoh.quirebind.core.model.FolioStyle;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.QuirePage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Assigns logical page numbers (folios) to a padded page list using a
 * {@link NumberingConfig}.
 *
 * <p>Page zones are identified structurally:
 * <ul>
 *   <li>Front matter: leading AESTHETIC and COMPLETION_BLANK pages before the first CONTENT page</li>
 *   <li>Body: all CONTENT pages</li>
 *   <li>Rear matter: trailing COMPLETION_BLANK and AESTHETIC pages after the last CONTENT page</li>
 *   <li>FILLER_BLANK pages never receive a folio</li>
 * </ul>
 *
 * <p>If there are no CONTENT pages, all pages are returned without folios.
 */
public final class FolioAssigner {

    private FolioAssigner() {
    }

    /**
     * Returns a new list with logical page numbers assigned according to {@code config}.
     *
     * @param pages  padded page list; must not be null
     * @param config numbering configuration; must not be null
     * @return an immutable list with the same physical positions and folio numbers added
     */
    public static List<QuirePage> assign(List<QuirePage> pages, NumberingConfig config) {
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(config, "config");

        int firstContent = -1;
        int lastContent = -1;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getPageType() == PageType.CONTENT) {
                if (firstContent < 0) {
                    firstContent = i;
                }
                lastContent = i;
            }
        }

        if (firstContent < 0) {
            return List.copyOf(pages);
        }

        List<QuirePage> result = new ArrayList<>(pages.size());
        int frontCounter = config.getFrontMatterStartNumber();
        int bodyCounter = config.getBodyStartNumber();
        int rearCounter = 1;
        boolean firstBody = true;

        for (int i = 0; i < pages.size(); i++) {
            QuirePage page = pages.get(i);
            PageType type = page.getPageType();

            if (type == PageType.FILLER_BLANK || type == PageType.COMPLETION_BLANK) {
                result.add(page);
                continue;
            }

            boolean isFrontMatter = i < firstContent;
            boolean isBody = type == PageType.CONTENT;
            boolean isRearMatter = i > lastContent;

            if (isFrontMatter) {
                Integer folio = nextFolio(config.getFrontMatterStyle(), frontCounter);
                result.add(folio != null ? page.toBuilder().logicalPageNumber(folio).build() : page);
                if (folio != null) {
                    frontCounter++;
                }
            } else if (isBody) {
                Integer folio = nextFolio(config.getBodyStyle(), bodyCounter);
                boolean suppress = firstBody && config.isSuppressFirstBodyFolio();
                result.add(folio != null && !suppress
                        ? page.toBuilder().logicalPageNumber(folio).build()
                        : page);
                if (folio != null) {
                    bodyCounter++;
                }
                firstBody = false;
            } else if (isRearMatter) {
                Integer folio = nextFolio(config.getRearMatterStyle(), rearCounter);
                result.add(folio != null ? page.toBuilder().logicalPageNumber(folio).build() : page);
                if (folio != null) {
                    rearCounter++;
                }
            } else {
                result.add(page);
            }
        }

        return List.copyOf(result);
    }

    private static Integer nextFolio(FolioStyle style, int counter) {
        return style == FolioStyle.NONE ? null : counter;
    }
}
