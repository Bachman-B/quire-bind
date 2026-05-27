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
package com.quirebind.guides.loader;

import com.quirebind.guides.model.BindingGuide;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuideLoaderTest {

    @Test
    void nullTechniqueIdThrows() {
        assertThrows(NullPointerException.class, () -> GuideLoader.load(null, "en"));
    }

    @Test
    void nullLocaleThrows() {
        assertThrows(NullPointerException.class, () -> GuideLoader.load("saddle_stitch", null));
    }

    @Test
    void unknownTechniqueThrowsIoException() {
        assertThrows(IOException.class, () -> GuideLoader.load("nonexistent", "en"));
    }

    @Test
    void unknownLocaleThrowsIoException() {
        assertThrows(IOException.class, () -> GuideLoader.load("saddle_stitch", "zz"));
    }

    @Test
    void loadSaddleStitchEnReturnsGuide() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertNotNull(guide);
    }

    @Test
    void loadSaddleStitchEnTechniqueIdIsSaddleStitch() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertEquals("saddle_stitch", guide.getMetadata().getTechniqueId());
    }

    @Test
    void loadSaddleStitchEnLocaleIsEn() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertEquals("en", guide.getMetadata().getLocale());
    }

    @Test
    void loadSaddleStitchEnTitleIsSet() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getMetadata().getTitle().isBlank());
    }

    @Test
    void loadSaddleStitchEnBodyIsNonBlank() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getBody().isBlank());
    }

    @Test
    void loadSaddleStitchEnSectionsArePresent() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getMetadata().getSections().isEmpty());
    }

    @Test
    void loadSaddleStitchEnToolsArePresent() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getMetadata().getTools().isEmpty());
    }

    @Test
    void loadSaddleStitchEnMaterialsArePresent() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getMetadata().getMaterials().isEmpty());
    }

    @Test
    void loadSaddleStitchEnReadingDirectionsIncludeLtr() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertTrue(guide.getMetadata().getReadingDirectionsSupported().contains("ltr"));
    }

    @Test
    void loadSaddleStitchEnGroupIsB() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertEquals("B", guide.getMetadata().getGroup());
    }

    @Test
    void loadSaddleStitchEnRelatedTechniquesPresent() throws IOException {
        BindingGuide guide = GuideLoader.load("saddle_stitch", "en");
        assertFalse(guide.getMetadata().getRelatedTechniques().isEmpty());
    }

    @Test
    void loadAllReturnsOneGuide() throws IOException {
        List<BindingGuide> guides = GuideLoader.loadAll();
        assertEquals(1, guides.size());
    }

    @Test
    void loadAllResultIsUnmodifiable() throws IOException {
        List<BindingGuide> guides = GuideLoader.loadAll();
        assertThrows(UnsupportedOperationException.class, () -> guides.add(null));
    }

    @Test
    void loadAllGuideHasSaddleStitchTechniqueId() throws IOException {
        List<BindingGuide> guides = GuideLoader.loadAll();
        assertEquals("saddle_stitch", guides.get(0).getMetadata().getTechniqueId());
    }

    @Test
    void parseMissingOpeningDelimiterThrowsIoException() {
        assertThrows(IOException.class,
                () -> GuideLoader.parse("no frontmatter here", "<test>"));
    }

    @Test
    void parseMissingClosingDelimiterThrowsIoException() {
        assertThrows(IOException.class,
                () -> GuideLoader.parse("---\ntechnique_id: saddle_stitch\n", "<test>"));
    }

    @Test
    void parseMinimalFrontmatterProducesGuide() throws IOException {
        String content = "---\ntechnique_id: saddle_stitch\nlocale: en\ntitle: Test\n---\n\nBody text.";
        BindingGuide guide = GuideLoader.parse(content, "<test>");
        assertEquals("saddle_stitch", guide.getMetadata().getTechniqueId());
        assertFalse(guide.getBody().isBlank());
    }

    @Test
    void parseEmptyYamlBlockProducesEmptyMetadata() throws IOException {
        String content = "---\n---\n\nBody text.";
        BindingGuide guide = GuideLoader.parse(content, "<test>");
        assertEquals("", guide.getMetadata().getTechniqueId());
    }
}
