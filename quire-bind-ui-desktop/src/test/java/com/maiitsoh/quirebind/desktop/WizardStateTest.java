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
package com.maiitsoh.quirebind.desktop;

import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.desktop.state.WizardState;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WizardStateTest {

    @Test
    void defaultsAreSet() {
        WizardState state = new WizardState();
        assertEquals(BindingTechnique.SADDLE_STITCH, state.getTechnique());
        assertEquals(PaperSize.A4, state.getPaperSize());
        assertEquals(16, state.getPagesPerSignature());
        assertEquals(ReadingDirection.LTR, state.getReadingDirection());
        assertEquals(0.0, state.getPaperThicknessMm());
        assertFalse(state.hasInputPdf());
        assertFalse(state.hasImpositionResult());
        assertNull(state.getOutputPdf());
    }

    @Test
    void addInputPdfAppendsAndHasInputPdfBecomesTrue() {
        WizardState state = new WizardState();
        assertFalse(state.hasInputPdf());
        assertNull(state.getInputPdf());

        Path p1 = Path.of("/tmp/a.pdf");
        Path p2 = Path.of("/tmp/b.pdf");
        state.addInputPdf(p1);
        state.addInputPdf(p2);

        assertTrue(state.hasInputPdf());
        assertEquals(p1, state.getInputPdf());
        assertEquals(2, state.getInputPdfs().size());
        assertEquals(p2, state.getInputPdfs().get(1));
    }

    @Test
    void removeInputPdfReducesList() {
        WizardState state = new WizardState();
        state.addInputPdf(Path.of("/tmp/a.pdf"));
        state.addInputPdf(Path.of("/tmp/b.pdf"));
        state.removeInputPdf(0);
        assertEquals(1, state.getInputPdfs().size());
        assertEquals(Path.of("/tmp/b.pdf"), state.getInputPdfs().get(0));
    }

    @Test
    void resetClearsInputPdfs() {
        WizardState state = new WizardState();
        state.addInputPdf(Path.of("/tmp/a.pdf"));
        state.reset();
        assertFalse(state.hasInputPdf());
        assertTrue(state.getInputPdfs().isEmpty());
    }

    @Test
    void settersRoundTrip() {
        WizardState state = new WizardState();
        Path path = Path.of("/tmp/test.pdf");
        state.addInputPdf(path);
        state.setTechnique(BindingTechnique.COPTIC);
        state.setPaperSize(PaperSize.A5);
        state.setPagesPerSignature(8);
        state.setReadingDirection(ReadingDirection.RTL);
        state.setPaperThicknessMm(0.12);

        assertEquals(path, state.getInputPdf());
        assertEquals(BindingTechnique.COPTIC, state.getTechnique());
        assertEquals(PaperSize.A5, state.getPaperSize());
        assertEquals(8, state.getPagesPerSignature());
        assertEquals(ReadingDirection.RTL, state.getReadingDirection());
        assertEquals(0.12, state.getPaperThicknessMm());
        assertTrue(state.hasInputPdf());
    }
}
