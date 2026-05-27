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

import com.quire.core.model.QuirePage;
import com.quire.core.model.QuireProject;
import com.quire.core.model.Signature;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the full imposition pipeline for a {@link QuireProject}.
 *
 * <p>The pipeline is:
 * <ol>
 *   <li>{@link PagePaddingApplier#pad} — inserts aesthetic, completion, and filler pages</li>
 *   <li>{@link FolioAssigner#assign} — assigns logical page numbers</li>
 *   <li>{@link SignatureComposer#compose} — folds pages into signatures</li>
 * </ol>
 */
public final class ImpositionEngine {

    private ImpositionEngine() {
    }

    /**
     * Runs the full imposition pipeline for the given project.
     *
     * @param project the project to impose; must not be null
     * @return an immutable list of composed signatures
     * @throws UnsupportedOperationException if the project's layout is not FOLIO
     */
    public static List<Signature> impose(QuireProject project) {
        Objects.requireNonNull(project, "project");

        List<QuirePage> padded = PagePaddingApplier.pad(
                project.getPageSequence().getPages(),
                project.getPaddingConfig(),
                project.getImpositionGroup());

        List<QuirePage> numbered = FolioAssigner.assign(padded, project.getNumberingConfig());

        return SignatureComposer.compose(
                numbered,
                project.getImpositionGroup(),
                project.getLayout(),
                project.getPaddingConfig().getSignatureSize(),
                project.getReadingDirection());
    }
}
