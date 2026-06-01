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
package com.maiitsoh.quirebind.web;

import com.maiitsoh.quirebind.web.model.WebSession;
import com.maiitsoh.quirebind.web.model.WebSession.SourceEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WebSessionTest {

    private WebSession session() {
        return new WebSession();
    }

    private void add(WebSession s, String name) {
        s.addSource(Path.of("/tmp/" + name), name, 1);
    }

    @Test
    void addSourceAppendsInOrder() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        assertEquals(2, s.getSources().size());
        assertEquals("a.pdf", s.getSources().get(0).filename());
        assertEquals("b.pdf", s.getSources().get(1).filename());
    }

    @Test
    void removeSourceDeletesEntry() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        s.removeSource(0);
        assertEquals(1, s.getSources().size());
        assertEquals("b.pdf", s.getSources().get(0).filename());
    }

    @Test
    void removeSourceOutOfRangeIsNoOp() {
        WebSession s = session();
        add(s, "a.pdf");
        s.removeSource(5);
        assertEquals(1, s.getSources().size());
    }

    @Test
    void insertSourceAddsAtIndex() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "c.pdf");
        SourceEntry b = new SourceEntry(Path.of("/tmp/b.pdf"), "b.pdf", 1);
        s.insertSource(1, b);
        assertEquals(3, s.getSources().size());
        assertEquals("a.pdf", s.getSources().get(0).filename());
        assertEquals("b.pdf", s.getSources().get(1).filename());
        assertEquals("c.pdf", s.getSources().get(2).filename());
    }

    @Test
    void moveSourceUpSwapsWithPrevious() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        add(s, "c.pdf");
        s.moveSourceUp(1);
        assertEquals("b.pdf", s.getSources().get(0).filename());
        assertEquals("a.pdf", s.getSources().get(1).filename());
        assertEquals("c.pdf", s.getSources().get(2).filename());
    }

    @Test
    void moveSourceUpAtFirstIndexIsNoOp() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        s.moveSourceUp(0);
        assertEquals("a.pdf", s.getSources().get(0).filename());
        assertEquals("b.pdf", s.getSources().get(1).filename());
    }

    @Test
    void moveSourceDownSwapsWithNext() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        add(s, "c.pdf");
        s.moveSourceDown(1);
        assertEquals("a.pdf", s.getSources().get(0).filename());
        assertEquals("c.pdf", s.getSources().get(1).filename());
        assertEquals("b.pdf", s.getSources().get(2).filename());
    }

    @Test
    void moveSourceDownAtLastIndexIsNoOp() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        s.moveSourceDown(1);
        assertEquals("a.pdf", s.getSources().get(0).filename());
        assertEquals("b.pdf", s.getSources().get(1).filename());
    }

    @Test
    void hasSourcesTogglesCorrectly() {
        WebSession s = session();
        assertFalse(s.hasSources());
        add(s, "a.pdf");
        assertTrue(s.hasSources());
        s.removeSource(0);
        assertFalse(s.hasSources());
    }

    @Test
    void getOriginalFilenameReturnsFirstSource() {
        WebSession s = session();
        assertNull(s.getOriginalFilename());
        add(s, "first.pdf");
        add(s, "second.pdf");
        assertEquals("first.pdf", s.getOriginalFilename());
    }

    @Test
    void clearSourcesRemovesAll() {
        WebSession s = session();
        add(s, "a.pdf");
        add(s, "b.pdf");
        s.clearSources();
        assertFalse(s.hasSources());
    }
}
