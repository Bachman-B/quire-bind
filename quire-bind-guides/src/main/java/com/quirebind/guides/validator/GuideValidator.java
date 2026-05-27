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
package com.quirebind.guides.validator;

import com.quirebind.core.model.BindingTechnique;
import com.quirebind.guides.model.BindingGuide;
import com.quirebind.guides.model.GuideMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Validates a {@link BindingGuide} for structural completeness.
 *
 * <p>A guide is valid when:
 * <ul>
 *   <li>{@code techniqueId} is non-blank and matches a known {@link BindingTechnique}</li>
 *   <li>{@code locale} is non-blank</li>
 *   <li>{@code title} is non-blank</li>
 *   <li>The {@code sections} list is non-empty</li>
 *   <li>The Markdown {@code body} is non-blank</li>
 * </ul>
 */
public final class GuideValidator {

    private GuideValidator() {
    }

    /**
     * Validates the given guide and returns a report of any issues found.
     *
     * @param guide the guide to validate; must not be null
     * @return a {@link ValidationReport} describing whether the guide is valid
     */
    public static ValidationReport validate(BindingGuide guide) {
        Objects.requireNonNull(guide, "guide");
        List<String> issues = new ArrayList<>();
        GuideMetadata m = guide.getMetadata();

        if (m.getTechniqueId().isBlank()) {
            issues.add("techniqueId is blank");
        } else if (!isKnownTechnique(m.getTechniqueId())) {
            issues.add("unknown techniqueId: '" + m.getTechniqueId() + "'");
        }
        if (m.getLocale().isBlank()) {
            issues.add("locale is blank");
        }
        if (m.getTitle().isBlank()) {
            issues.add("title is blank");
        }
        if (m.getSections().isEmpty()) {
            issues.add("sections list is empty");
        }
        if (guide.getBody().isBlank()) {
            issues.add("markdown body is blank");
        }

        return new ValidationReport(issues.isEmpty(), List.copyOf(issues));
    }

    private static boolean isKnownTechnique(String techniqueId) {
        return Arrays.stream(BindingTechnique.values())
                .anyMatch(bt -> bt.name().equalsIgnoreCase(techniqueId));
    }

    /**
     * The result of validating a {@link BindingGuide}.
     *
     * @param valid  {@code true} if no issues were found
     * @param issues an unmodifiable list of human-readable issue descriptions
     */
    public record ValidationReport(boolean valid, List<String> issues) {
    }
}
