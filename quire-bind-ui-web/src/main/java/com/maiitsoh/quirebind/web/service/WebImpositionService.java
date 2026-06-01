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
package com.maiitsoh.quirebind.web.service;

import com.maiitsoh.quirebind.core.binding.BindingGroupMapper;
import com.maiitsoh.quirebind.core.creep.CreepCalculator;
import com.maiitsoh.quirebind.core.model.CreepConfig;
import com.maiitsoh.quirebind.core.imposition.FolioAssigner;
import com.maiitsoh.quirebind.core.imposition.PagePaddingApplier;
import com.maiitsoh.quirebind.core.imposition.SignatureComposer;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
import com.maiitsoh.quirebind.core.model.ImposedSheet;
import com.maiitsoh.quirebind.core.model.ImpositionGroup;
import com.maiitsoh.quirebind.core.model.ImpositionLayout;
import com.maiitsoh.quirebind.core.model.MarkConfig;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PaddingConfig;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.SewingConfig;
import com.maiitsoh.quirebind.core.model.Signature;
import com.maiitsoh.quirebind.core.pdf.PdfImpositionWriter;
import com.maiitsoh.quirebind.web.model.WebSession;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Drives the quire-core imposition pipeline for a web session. */
@Service
public class WebImpositionService {

    /**
     * Reads the source PDF and computes the full imposition for the given session.
     *
     * @param session the current wizard session
     * @throws IOException if the source PDF cannot be read
     */
    public void impose(WebSession session) throws IOException {
        PageSequence seq = session.getPageSequence();
        if (seq == null) {
            throw new IllegalStateException("No page sequence in session");
        }
        ImpositionGroup group = BindingGroupMapper.groupFor(session.getTechnique());

        PaddingConfig paddingConfig = PaddingConfig.builder()
            .signatureSize(session.getSignatureSize())
            .completionFront(0)
            .completionRear(0)
            .build();

        NumberingConfig numberingConfig = buildNumberingConfig(session);
        List<Signature> all = imposeWithZones(seq, group, paddingConfig, numberingConfig,
            session.getReadingDirection(), session.getFrontMatterPageCount(),
            session.getRearMatterPageCount());
        session.setImpositionResult(all);
    }

    /**
     * Writes the imposed PDF to the given output stream using the session's marks and numbering.
     *
     * @param session      the current wizard session (must have an imposition result)
     * @param outputStream the stream to write the imposed PDF to
     * @throws IOException              if writing fails
     * @throws IllegalStateException    if imposition has not been computed
     */
    public void export(WebSession session, OutputStream outputStream) throws IOException {
        if (!session.hasImpositionResult()) {
            throw new IllegalStateException("Imposition has not been computed");
        }
        Path tempOut = Files.createTempFile("quire-web-export-", ".pdf");
        try {
            SewingConfig sewingConfig = session.isSewingHoles()
                ? SewingConfig.builder()
                    .style(session.getSewingStyle())
                    .holeCount(session.getSewingHoleCount())
                    .endMarginMm(session.getSewingEndMarginMm())
                    .bandCount(session.getSewingBandCount())
                    .bandWidthMm(session.getSewingBandWidthMm())
                    .build()
                : null;
            MarkConfig markConfig = MarkConfig.builder()
                .foldLines(session.isFoldLines())
                .signatureProofMarkers(session.isStitchMarks())
                .sewingHoles(session.isSewingHoles())
                .sewingConfig(sewingConfig)
                .trimLines(session.isTrimLines())
                .build();
            NumberingConfig numberingConfig = buildNumberingConfig(session);
            CreepConfig creepConfig = null;
            if (session.isApplyCreep() && session.getPaperThicknessMm() > 0) {
                CreepConfig base = CreepConfig.builder()
                    .paperThicknessMm(session.getPaperThicknessMm())
                    .applyToOutput(true)
                    .build();
                creepConfig = CreepCalculator.calculate(session.getImpositionResult(), base);
            }
            ImpositionGroup group = BindingGroupMapper.groupFor(session.getTechnique());
            if (group == ImpositionGroup.A) {
                PdfImpositionWriter.writeGroupA(
                    session.getImpositionResult(),
                    session.getSourceDocPaths(),
                    tempOut,
                    session.getPaperSize(),
                    markConfig,
                    numberingConfig,
                    creepConfig);
            } else {
                PdfImpositionWriter.write(
                    session.getImpositionResult(),
                    session.getSourceDocPaths(),
                    tempOut,
                    session.getPaperSize(),
                    markConfig,
                    numberingConfig,
                    creepConfig);
            }
            Files.copy(tempOut, outputStream);
        } finally {
            Files.deleteIfExists(tempOut);
        }
    }

    /**
     * Returns a summary of the imposition result as a list of {@link SignatureSummary} rows.
     *
     * @param session the current wizard session
     * @return an unmodifiable list of signature summary rows
     */
    public List<SignatureSummary> summarise(WebSession session) {
        List<Signature> all = session.getImpositionResult();
        if (all == null) {
            return List.of();
        }
        PageSequence seq = session.getPageSequence();
        if (seq == null) {
            return List.of();
        }
        ImpositionGroup group = BindingGroupMapper.groupFor(session.getTechnique());
        int frontCount = session.getFrontMatterPageCount();
        int rearCount = session.getRearMatterPageCount();
        int total = seq.pageCount();
        int bodyStart = Math.min(frontCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearCount);
        int frontSigCount = countFrontSigs(seq, group, frontCount, total);
        int bodySigCount = all.size() - frontSigCount - countRearSigs(seq, group, rearCount, bodyEnd, total);

        List<SignatureSummary> rows = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            Signature s = all.get(i);
            String zone;
            if (i < frontSigCount) {
                zone = "Front matter";
            } else if (i < frontSigCount + bodySigCount) {
                zone = "Body";
            } else {
                zone = "Rear matter";
            }
            int pages = s.getSheets().stream()
                .mapToInt(sh -> sh.getFrontPages().size() + sh.getBackPages().size())
                .sum();
            rows.add(new SignatureSummary(zone, i + 1, pages, s.getSheets().size()));
        }
        return List.copyOf(rows);
    }

    private static int countFrontSigs(PageSequence seq, ImpositionGroup group,
            int frontCount, int total) {
        int bodyStart = Math.min(frontCount, total);
        if (bodyStart == 0) {
            return 0;
        }
        return SignatureComposer.compose(
            seq.getPages().subList(0, bodyStart),
            ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR).size();
    }

    private static int countRearSigs(PageSequence seq, ImpositionGroup group,
            int rearCount, int bodyEnd, int total) {
        if (bodyEnd >= total) {
            return 0;
        }
        return SignatureComposer.compose(
            seq.getPages().subList(bodyEnd, total),
            ImpositionGroup.B, ImpositionLayout.FOLIO, 1, ReadingDirection.LTR).size();
    }

    private static List<Signature> imposeWithZones(
            PageSequence seq,
            ImpositionGroup group,
            PaddingConfig paddingConfig,
            NumberingConfig numberingConfig,
            ReadingDirection direction,
            int frontMatterPageCount,
            int rearMatterPageCount) {
        int total = seq.pageCount();
        int bodyStart = Math.min(frontMatterPageCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearMatterPageCount);

        List<QuirePage> frontPages = new ArrayList<>(seq.getPages().subList(0, bodyStart));
        List<QuirePage> bodyRaw = new ArrayList<>(seq.getPages().subList(bodyStart, bodyEnd));
        List<QuirePage> rearPages = new ArrayList<>(seq.getPages().subList(bodyEnd, total));

        List<QuirePage> bodyPadded = PagePaddingApplier.pad(bodyRaw, paddingConfig, group);

        List<QuirePage> combined = new ArrayList<>(
            frontPages.size() + bodyPadded.size() + rearPages.size());
        combined.addAll(frontPages);
        combined.addAll(bodyPadded);
        combined.addAll(rearPages);
        List<QuirePage> numbered = FolioAssigner.assign(combined, numberingConfig);

        int fSize = frontPages.size();
        int bSize = bodyPadded.size();
        List<QuirePage> nFront = new ArrayList<>(numbered.subList(0, fSize));
        List<QuirePage> nBody = new ArrayList<>(numbered.subList(fSize, fSize + bSize));
        List<QuirePage> nRear = new ArrayList<>(numbered.subList(fSize + bSize, numbered.size()));

        List<Signature> all = new ArrayList<>();
        int offset = 0;
        if (!nFront.isEmpty()) {
            for (Signature s : SignatureComposer.compose(
                    nFront, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, direction)) {
                all.add(reindex(s, offset++));
            }
        }
        if (!nBody.isEmpty()) {
            for (Signature s : SignatureComposer.compose(
                    nBody, group, ImpositionLayout.FOLIO,
                    paddingConfig.getSignatureSize(), direction)) {
                all.add(reindex(s, offset++));
            }
        }
        if (!nRear.isEmpty()) {
            for (Signature s : SignatureComposer.compose(
                    nRear, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, direction)) {
                all.add(reindex(s, offset++));
            }
        }
        return List.copyOf(all);
    }

    private static Signature reindex(Signature sig, int idx) {
        List<ImposedSheet> sheets = sig.getSheets().stream()
            .map(sh -> ImposedSheet.builder()
                .sheetIndex(sh.getSheetIndex())
                .signatureIndex(idx)
                .frontPages(sh.getFrontPages())
                .backPages(sh.getBackPages())
                .build())
            .toList();
        return Signature.builder()
            .signatureIndex(idx)
            .sheets(sheets)
            .logicalPageNumbers(sig.getLogicalPageNumbers())
            .build();
    }

    private static NumberingConfig buildNumberingConfig(WebSession session) {
        return NumberingConfig.builder()
            .frontMatterStyle(session.getFrontMatterFolioStyle() != null
                ? session.getFrontMatterFolioStyle() : FolioStyle.NONE)
            .bodyStyle(session.getBodyFolioStyle() != null
                ? session.getBodyFolioStyle() : FolioStyle.ARABIC)
            .rearMatterStyle(session.getRearMatterFolioStyle() != null
                ? session.getRearMatterFolioStyle() : FolioStyle.NONE)
            .frontMatterStartNumber(session.getFrontMatterStartNumber())
            .bodyStartNumber(session.getBodyStartNumber())
            .rearMatterStartNumber(session.getRearMatterStartNumber())
            .suppressFirstBodyFolio(session.isSuppressFirstFolio())
            .folioPosition(session.getFolioPosition() != null
                ? session.getFolioPosition() : FolioPosition.BOTTOM_OUTER)
            .build();
    }

    /** Summarises one signature for display in the imposition table. */
    public record SignatureSummary(String zone, int index, int pages, int sheets) {
    }
}
