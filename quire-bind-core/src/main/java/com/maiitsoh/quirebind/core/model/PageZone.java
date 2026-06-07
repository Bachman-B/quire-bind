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
 * The numbering zone a page belongs to within a document.
 *
 * <p>Used by {@link QuirePage} to record the zone assigned during folio numbering,
 * so that the PDF renderer can enforce per-zone {@link FolioStyle} at export time
 * independently of what style was active when logical page numbers were first assigned.
 */
public enum PageZone {

    /** Pages before the first content page (aesthetic / endpaper). */
    FRONT_MATTER,

    /** Content pages forming the body of the document. */
    BODY,

    /** Pages after the last content page (aesthetic / endpaper). */
    REAR_MATTER
}
