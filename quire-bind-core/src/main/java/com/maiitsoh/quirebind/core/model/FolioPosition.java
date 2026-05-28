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

/**
 * The printed position of the folio (page number) on the page.
 *
 * <p>Position is reading-direction aware: INNER is the spine side and OUTER is the
 * fore-edge side, regardless of LTR or RTL. The default for most books is
 * {@link #BOTTOM_OUTER}.
 */
public enum FolioPosition {

    /** Top of page, spine-side edge. */
    TOP_INNER,

    /** Top of page, centred. */
    TOP_CENTER,

    /** Top of page, fore-edge side (running-head position). */
    TOP_OUTER,

    /** Bottom of page, spine-side edge. */
    BOTTOM_INNER,

    /** Bottom of page, centred. */
    BOTTOM_CENTER,

    /**
     * Bottom of page, fore-edge side. The most common typographic convention for
     * traditional page folios.
     */
    BOTTOM_OUTER
}
