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
package com.maiitsoh.quirebind.desktop.state;

import com.maiitsoh.quirebind.batch.model.BatchConfig;
import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.SewingConfig;
import com.maiitsoh.quirebind.core.model.Signature;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mutable state shared across wizard steps within a single session. */
public final class WizardState {

    /** Whether the user is working with a single PDF or a batch .quire file. */
    public enum WizardMode {
        SINGLE_PDF,
        BATCH
    }

    /** Where completion blank pages are inserted to fill the last signature. */
    public enum PaddingPosition {
        BEFORE,
        AFTER
    }

    /** How output paths are derived in batch template mode. */
    public enum BatchOutputMode {
        SUFFIX,
        OUTPUT_DIR
    }

    private WizardMode mode = WizardMode.SINGLE_PDF;
    private PaddingPosition paddingPosition = PaddingPosition.AFTER;

    // Single PDF mode — supports multiple source PDFs combined in order
    private List<Path> inputPdfs = new ArrayList<>();
    private PageSequence pageSequence;

    // Batch mode
    private Path batchConfigPath;
    private BatchConfig batchConfig;
    private List<Path> batchInputPdfs = new ArrayList<>();
    private BatchOutputMode batchOutputMode = BatchOutputMode.SUFFIX;
    private String batchOutputSuffix = "-imposed";
    private Path batchOutputDir;

    // Binding options (shared)
    private BindingTechnique technique = BindingTechnique.SADDLE_STITCH;
    private PaperSize paperSize = PaperSize.A4;
    private int pagesPerSignature = 16;
    private ReadingDirection readingDirection = ReadingDirection.LTR;
    private double paperThicknessMm = 0.0;

    // Output marks
    private boolean foldLines = false;
    private boolean stitchMarks = false;
    private boolean sewingHoles = false;
    private SewingConfig.SewingStyle sewingStyle = SewingConfig.SewingStyle.SIMPLE;
    private int sewingHoleCount = 5;
    private double sewingEndMarginMm = 15.0;
    private int sewingBandCount = 3;
    private double sewingBandWidthMm = 10.0;
    private boolean trimLines = false;

    // Front/rear matter page counts (always multiples of 4; 0 = none)
    private int frontMatterPageCount = 0;
    private int rearMatterPageCount = 0;

    // Page numbering
    private FolioStyle bodyFolioStyle = FolioStyle.ARABIC;
    private FolioStyle frontMatterFolioStyle = FolioStyle.NONE;
    private FolioStyle rearMatterFolioStyle = FolioStyle.NONE;
    private int frontMatterStartNumber = 1;
    private int bodyStartNumber = 1;
    private int rearMatterStartNumber = 1;
    private boolean suppressFirstFolio = false;
    private FolioPosition folioPosition = FolioPosition.BOTTOM_OUTER;

    // Imposition result
    private List<Signature> impositionResult;

    // Export
    private Path outputPdf;

    /** Returns the current wizard mode. */
    public WizardMode getMode() {
        return mode;
    }

    /** Sets the wizard mode. */
    public void setMode(WizardMode mode) {
        this.mode = mode;
    }

    /** Returns where completion blanks are inserted to fill the last signature. */
    public PaddingPosition getPaddingPosition() {
        return paddingPosition;
    }

    /** Sets where completion blanks are inserted. */
    public void setPaddingPosition(PaddingPosition paddingPosition) {
        this.paddingPosition = paddingPosition;
    }

    /** Returns the mutable list of source PDF paths in load order. */
    public List<Path> getInputPdfs() {
        return inputPdfs;
    }

    /** Appends a source PDF path. */
    public void addInputPdf(Path path) {
        inputPdfs.add(path);
    }

    /** Removes the source at the given index. No-op if the index is out of range. */
    public void removeInputPdf(int index) {
        if (index >= 0 && index < inputPdfs.size()) {
            inputPdfs.remove(index);
        }
    }

    /**
     * Returns the first source PDF path, or {@code null} if none has been added.
     * Retained for compatibility — prefer {@link #getInputPdfs()} for multi-source use.
     */
    public Path getInputPdf() {
        return inputPdfs.isEmpty() ? null : inputPdfs.get(0);
    }

    /**
     * Returns a map from source document ID (path string) to path, suitable for passing
     * to {@link com.maiitsoh.quirebind.core.pdf.PdfImpositionWriter}.
     */
    public Map<String, Path> getSourceDocPaths() {
        Map<String, Path> map = new LinkedHashMap<>();
        for (Path p : inputPdfs) {
            map.put(p.toString(), p);
        }
        return map;
    }

    /** Returns the mutable page sequence, or {@code null} if not yet loaded. */
    public PageSequence getPageSequence() {
        return pageSequence;
    }

    /** Sets the page sequence loaded from the source PDF. */
    public void setPageSequence(PageSequence pageSequence) {
        this.pageSequence = pageSequence;
    }

    /** Returns the path to the .quire batch config file. */
    public Path getBatchConfigPath() {
        return batchConfigPath;
    }

    /** Sets the .quire batch config file path. */
    public void setBatchConfigPath(Path batchConfigPath) {
        this.batchConfigPath = batchConfigPath;
    }

    /** Returns the parsed batch configuration, or {@code null} if not yet loaded. */
    public BatchConfig getBatchConfig() {
        return batchConfig;
    }

    /** Sets the parsed batch configuration. */
    public void setBatchConfig(BatchConfig batchConfig) {
        this.batchConfig = batchConfig;
    }

    /** Returns the mutable list of input PDFs for batch template mode. */
    public List<Path> getBatchInputPdfs() {
        return batchInputPdfs;
    }

    /** Returns the output naming mode for batch template mode. */
    public BatchOutputMode getBatchOutputMode() {
        return batchOutputMode;
    }

    /** Sets the output naming mode for batch template mode. */
    public void setBatchOutputMode(BatchOutputMode batchOutputMode) {
        this.batchOutputMode = batchOutputMode;
    }

    /** Returns the suffix appended before the file extension in SUFFIX output mode. */
    public String getBatchOutputSuffix() {
        return batchOutputSuffix;
    }

    /** Sets the output suffix. */
    public void setBatchOutputSuffix(String batchOutputSuffix) {
        this.batchOutputSuffix = batchOutputSuffix;
    }

    /** Returns the output directory for OUTPUT_DIR mode, or {@code null} if not set. */
    public Path getBatchOutputDir() {
        return batchOutputDir;
    }

    /** Sets the output directory. */
    public void setBatchOutputDir(Path batchOutputDir) {
        this.batchOutputDir = batchOutputDir;
    }

    /** Returns the chosen binding technique. */
    public BindingTechnique getTechnique() {
        return technique;
    }

    /** Sets the binding technique. */
    public void setTechnique(BindingTechnique technique) {
        this.technique = technique;
    }

    /** Returns the chosen output paper size. */
    public PaperSize getPaperSize() {
        return paperSize;
    }

    /** Sets the output paper size. */
    public void setPaperSize(PaperSize paperSize) {
        this.paperSize = paperSize;
    }

    /** Returns the number of pages per signature (relevant for group C techniques). */
    public int getPagesPerSignature() {
        return pagesPerSignature;
    }

    /** Sets the number of pages per signature. */
    public void setPagesPerSignature(int pagesPerSignature) {
        this.pagesPerSignature = pagesPerSignature;
    }

    /** Returns the reading direction. */
    public ReadingDirection getReadingDirection() {
        return readingDirection;
    }

    /** Sets the reading direction. */
    public void setReadingDirection(ReadingDirection readingDirection) {
        this.readingDirection = readingDirection;
    }

    /** Returns the paper thickness in mm used for creep calculation, or {@code 0.0} to skip. */
    public double getPaperThicknessMm() {
        return paperThicknessMm;
    }

    /** Sets the paper thickness in mm. */
    public void setPaperThicknessMm(double paperThicknessMm) {
        this.paperThicknessMm = paperThicknessMm;
    }

    /** Returns whether fold lines should be printed on the output. */
    public boolean isFoldLines() {
        return foldLines;
    }

    /** Sets whether fold lines should be printed. */
    public void setFoldLines(boolean foldLines) {
        this.foldLines = foldLines;
    }

    /** Returns whether stitch marks should be printed on the output. */
    public boolean isStitchMarks() {
        return stitchMarks;
    }

    /** Sets whether stitch marks should be printed. */
    public void setStitchMarks(boolean stitchMarks) {
        this.stitchMarks = stitchMarks;
    }

    /** Returns whether sewing holes should be marked on the output. */
    public boolean isSewingHoles() {
        return sewingHoles;
    }

    /** Sets whether sewing holes should be marked. */
    public void setSewingHoles(boolean sewingHoles) {
        this.sewingHoles = sewingHoles;
    }

    /** Returns the sewing style (SIMPLE or BANDED). */
    public SewingConfig.SewingStyle getSewingStyle() {
        return sewingStyle;
    }

    /** Sets the sewing style. */
    public void setSewingStyle(SewingConfig.SewingStyle sewingStyle) {
        this.sewingStyle = sewingStyle;
    }

    /** Returns the number of sewing holes. */
    public int getSewingHoleCount() {
        return sewingHoleCount;
    }

    /** Sets the number of sewing holes. */
    public void setSewingHoleCount(int sewingHoleCount) {
        this.sewingHoleCount = sewingHoleCount;
    }

    /** Returns the distance in mm from head/tail of the spine to the first/last hole. */
    public double getSewingEndMarginMm() {
        return sewingEndMarginMm;
    }

    /** Sets the end margin in mm for sewing hole placement. */
    public void setSewingEndMarginMm(double sewingEndMarginMm) {
        this.sewingEndMarginMm = sewingEndMarginMm;
    }

    /** Returns the number of bands/tapes for banded sewing. */
    public int getSewingBandCount() {
        return sewingBandCount;
    }

    /** Sets the number of bands/tapes. */
    public void setSewingBandCount(int sewingBandCount) {
        this.sewingBandCount = sewingBandCount;
    }

    /** Returns the band/tape width in mm. */
    public double getSewingBandWidthMm() {
        return sewingBandWidthMm;
    }

    /** Sets the band/tape width in mm. */
    public void setSewingBandWidthMm(double sewingBandWidthMm) {
        this.sewingBandWidthMm = sewingBandWidthMm;
    }

    /** Returns whether trim lines should be printed on the output. */
    public boolean isTrimLines() {
        return trimLines;
    }

    /** Sets whether trim lines should be printed. */
    public void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
    }

    /** Returns the number of front matter pages (always a multiple of 4). */
    public int getFrontMatterPageCount() {
        return frontMatterPageCount;
    }

    /** Sets the number of front matter pages. */
    public void setFrontMatterPageCount(int frontMatterPageCount) {
        this.frontMatterPageCount = frontMatterPageCount;
    }

    /** Returns the number of rear matter pages (always a multiple of 4). */
    public int getRearMatterPageCount() {
        return rearMatterPageCount;
    }

    /** Sets the number of rear matter pages. */
    public void setRearMatterPageCount(int rearMatterPageCount) {
        this.rearMatterPageCount = rearMatterPageCount;
    }

    /** Returns the folio style for body (content) pages. */
    public FolioStyle getBodyFolioStyle() {
        return bodyFolioStyle;
    }

    /** Sets the folio style for body pages. */
    public void setBodyFolioStyle(FolioStyle bodyFolioStyle) {
        this.bodyFolioStyle = bodyFolioStyle;
    }

    /** Returns the folio style for front matter pages. */
    public FolioStyle getFrontMatterFolioStyle() {
        return frontMatterFolioStyle;
    }

    /** Sets the folio style for front matter pages. */
    public void setFrontMatterFolioStyle(FolioStyle frontMatterFolioStyle) {
        this.frontMatterFolioStyle = frontMatterFolioStyle;
    }

    /** Returns the folio style for rear matter pages. */
    public FolioStyle getRearMatterFolioStyle() {
        return rearMatterFolioStyle;
    }

    /** Sets the folio style for rear matter pages. */
    public void setRearMatterFolioStyle(FolioStyle rearMatterFolioStyle) {
        this.rearMatterFolioStyle = rearMatterFolioStyle;
    }

    /** Returns the start number for the first front matter page. */
    public int getFrontMatterStartNumber() {
        return frontMatterStartNumber;
    }

    /** Sets the start number for front matter pages. */
    public void setFrontMatterStartNumber(int frontMatterStartNumber) {
        this.frontMatterStartNumber = frontMatterStartNumber;
    }

    /** Returns the start number assigned to the first body page. */
    public int getBodyStartNumber() {
        return bodyStartNumber;
    }

    /** Sets the start number for the first body page. */
    public void setBodyStartNumber(int bodyStartNumber) {
        this.bodyStartNumber = bodyStartNumber;
    }

    /** Returns the start number assigned to the first rear matter page. */
    public int getRearMatterStartNumber() {
        return rearMatterStartNumber;
    }

    /** Sets the start number for the first rear matter page. */
    public void setRearMatterStartNumber(int rearMatterStartNumber) {
        this.rearMatterStartNumber = rearMatterStartNumber;
    }

    /** Returns whether the folio is suppressed on the first body page. */
    public boolean isSuppressFirstFolio() {
        return suppressFirstFolio;
    }

    /** Sets whether the folio is suppressed on the first body page. */
    public void setSuppressFirstFolio(boolean suppressFirstFolio) {
        this.suppressFirstFolio = suppressFirstFolio;
    }

    /** Returns the horizontal position of the printed folio. */
    public FolioPosition getFolioPosition() {
        return folioPosition;
    }

    /** Sets the horizontal position of the printed folio. */
    public void setFolioPosition(FolioPosition folioPosition) {
        this.folioPosition = folioPosition;
    }

    /** Returns the computed imposition result, or {@code null} if imposition has not run yet. */
    public List<Signature> getImpositionResult() {
        return impositionResult;
    }

    /** Stores the computed imposition result. */
    public void setImpositionResult(List<Signature> impositionResult) {
        this.impositionResult = impositionResult;
    }

    /** Returns the chosen output PDF path, or {@code null} if none has been chosen. */
    public Path getOutputPdf() {
        return outputPdf;
    }

    /** Sets the output PDF path. */
    public void setOutputPdf(Path outputPdf) {
        this.outputPdf = outputPdf;
    }

    /** Returns {@code true} if at least one source PDF has been added. */
    public boolean hasInputPdf() {
        return !inputPdfs.isEmpty();
    }

    /** Returns {@code true} if a .quire batch config has been loaded. */
    public boolean hasBatchConfig() {
        return batchConfig != null;
    }

    /** Returns {@code true} if imposition has already been computed. */
    public boolean hasImpositionResult() {
        return impositionResult != null;
    }

    /** Clears all state, resetting to defaults for a new project. */
    public void reset() {
        mode = WizardMode.SINGLE_PDF;
        paddingPosition = PaddingPosition.AFTER;
        inputPdfs = new ArrayList<>();
        pageSequence = null;
        batchConfigPath = null;
        batchConfig = null;
        batchInputPdfs = new ArrayList<>();
        batchOutputMode = BatchOutputMode.SUFFIX;
        batchOutputSuffix = "-imposed";
        batchOutputDir = null;
        technique = BindingTechnique.SADDLE_STITCH;
        paperSize = PaperSize.A4;
        pagesPerSignature = 16;
        readingDirection = ReadingDirection.LTR;
        paperThicknessMm = 0.0;
        foldLines = false;
        stitchMarks = false;
        sewingHoles = false;
        sewingStyle = SewingConfig.SewingStyle.SIMPLE;
        sewingHoleCount = 5;
        sewingEndMarginMm = 15.0;
        sewingBandCount = 3;
        sewingBandWidthMm = 10.0;
        trimLines = false;
        frontMatterPageCount = 0;
        rearMatterPageCount = 0;
        bodyFolioStyle = FolioStyle.ARABIC;
        frontMatterFolioStyle = FolioStyle.NONE;
        rearMatterFolioStyle = FolioStyle.NONE;
        frontMatterStartNumber = 1;
        bodyStartNumber = 1;
        rearMatterStartNumber = 1;
        suppressFirstFolio = false;
        folioPosition = FolioPosition.BOTTOM_OUTER;
        impositionResult = null;
        outputPdf = null;
    }
}
