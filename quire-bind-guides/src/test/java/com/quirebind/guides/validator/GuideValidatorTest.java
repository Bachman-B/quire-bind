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

import com.quirebind.guides.loader.GuideLoader;
import com.quirebind.guides.model.BindingGuide;
import com.quirebind.guides.model.GuideMetadata;
import com.quirebind.guides.model.GuideSection;
import com.quirebind.guides.validator.GuideValidator.ValidationReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuideValidatorTest {

    private static BindingGuide validGuide() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("saddle_stitch")
                .locale("en")
                .title("Saddle stitch")
                .sections(List.of(new GuideSection("overview", "Overview")))
                .build();
        return new BindingGuide(meta, "Some markdown body text.");
    }

    @Test
    void nullGuideThrows() {
        assertThrows(NullPointerException.class, () -> GuideValidator.validate(null));
    }

    @Test
    void validGuideReturnsValidReport() {
        ValidationReport report = GuideValidator.validate(validGuide());
        assertTrue(report.valid());
        assertTrue(report.issues().isEmpty());
    }

    @Test
    void bundledSaddleStitchGuideIsValid() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        ValidationReport report = GuideValidator.validate(guide);
        assertTrue(report.valid(), "Issues: " + report.issues());
    }

    @Test
    void blankTechniqueIdIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("")
                .locale("en")
                .title("Test")
                .sections(List.of(new GuideSection("s", "S")))
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("techniqueId")));
    }

    @Test
    void unknownTechniqueIdIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("origami_fold")
                .locale("en")
                .title("Test")
                .sections(List.of(new GuideSection("s", "S")))
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("unknown techniqueId")));
    }

    @Test
    void blankLocaleIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("saddle_stitch")
                .locale("")
                .title("Test")
                .sections(List.of(new GuideSection("s", "S")))
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("locale")));
    }

    @Test
    void blankTitleIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("saddle_stitch")
                .locale("en")
                .title("")
                .sections(List.of(new GuideSection("s", "S")))
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("title")));
    }

    @Test
    void emptySectionsIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("saddle_stitch")
                .locale("en")
                .title("Test")
                .sections(List.of())
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("sections")));
    }

    @Test
    void blankBodyIsInvalid() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("saddle_stitch")
                .locale("en")
                .title("Test")
                .sections(List.of(new GuideSection("s", "S")))
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "   "));
        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.contains("body")));
    }

    @Test
    void multipleIssuesCollectedTogether() {
        GuideMetadata meta = GuideMetadata.builder()
                .techniqueId("")
                .locale("")
                .title("")
                .sections(List.of())
                .build();
        ValidationReport report = GuideValidator.validate(new BindingGuide(meta, ""));
        assertFalse(report.valid());
        assertTrue(report.issues().size() >= 4);
    }

    @Test
    void issuesListIsUnmodifiable() {
        ValidationReport report = GuideValidator.validate(validGuide());
        assertThrows(UnsupportedOperationException.class, () -> report.issues().add("x"));
    }

    @Test
    void allKnownTechniqueIdsAreAccepted() {
        for (com.quirebind.core.model.BindingTechnique bt
                : com.quirebind.core.model.BindingTechnique.values()) {
            GuideMetadata meta = GuideMetadata.builder()
                    .techniqueId(bt.name().toLowerCase())
                    .locale("en")
                    .title("Test")
                    .sections(List.of(new GuideSection("s", "S")))
                    .build();
            ValidationReport report = GuideValidator.validate(new BindingGuide(meta, "body"));
            assertFalse(report.issues().stream().anyMatch(i -> i.contains("unknown techniqueId")),
                    "Expected " + bt.name() + " to be accepted");
        }
    }
}
