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
package com.maiitsoh.quirebind.desktop.controller;

import com.maiitsoh.quirebind.batch.parser.QuireFileParser;
import com.maiitsoh.quirebind.core.binding.BindingGroupMapper;
import com.maiitsoh.quirebind.core.imposition.ImpositionEngine;
import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.CreepConfig;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.maiitsoh.quirebind.core.model.ImpositionGroup;
import com.maiitsoh.quirebind.core.model.ImpositionLayout;
import com.maiitsoh.quirebind.core.model.MarkConfig;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PaddingConfig;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.QuireProject;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.Signature;
import com.maiitsoh.quirebind.core.pdf.PdfImpositionWriter;
import com.maiitsoh.quirebind.core.pdf.PdfPageLoader;
import com.maiitsoh.quirebind.desktop.diagram.BindingTechniqueDiagram;
import com.maiitsoh.quirebind.desktop.state.WizardState;
import com.maiitsoh.quirebind.desktop.state.WizardState.PaddingPosition;
import com.maiitsoh.quirebind.desktop.state.WizardState.WizardMode;
import com.maiitsoh.quirebind.desktop.template.QuireTemplateWriter;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

/** Controls the four-step imposition wizard in the main window. */
public final class MainController implements Initializable {

    private static final int TOTAL_STEPS = 4;

    // ── Wizard shell ─────────────────────────────────────────────────────────
    @FXML private StackPane wizardStack;
    @FXML private VBox stepLoad;
    @FXML private VBox stepOptions;
    @FXML private VBox stepPreview;
    @FXML private VBox stepExport;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Label stepIndicatorLabel;
    @FXML private Label statusLabel;

    // ── Step 1: Load ─────────────────────────────────────────────────────────
    @FXML private ToggleButton singlePdfToggle;
    @FXML private ToggleButton batchToggle;
    @FXML private VBox singlePdfPane;
    @FXML private VBox batchPane;
    @FXML private TextField pdfPathField;
    @FXML private Label pageCountLabel;
    @FXML private TextField quirePathField;
    @FXML private Label jobCountLabel;

    // ── Step 2: Options ──────────────────────────────────────────────────────
    @FXML private ComboBox<BindingTechnique> techniqueCombo;
    @FXML private ComboBox<PaperSize> paperSizeCombo;
    @FXML private Spinner<Integer> sigSizeSpinner;
    @FXML private Label sigSizeLabel;
    @FXML private ComboBox<ReadingDirection> directionCombo;
    @FXML private TextField thicknessField;
    @FXML private StackPane diagramPane;
    @FXML private ToggleButton padAfterToggle;
    @FXML private ToggleButton padBeforeToggle;
    @FXML private VBox paddingPositionPane;

    // ── Step 3: Preview / edit ────────────────────────────────────────────────
    @FXML private ListView<PageItem> pageListView;
    @FXML private Label previewSummaryLabel;
    @FXML private Label overflowLabel;
    @FXML private Spinner<Integer> frontMatterSpinner;
    @FXML private Spinner<Integer> rearMatterSpinner;
    @FXML private javafx.scene.control.CheckBox thumbnailCheck;
    @FXML private ImageView thumbnailView;
    @FXML private Label thumbnailPlaceholder;
    @FXML private TextField pageLabelField;

    // ── Step 4: Export ───────────────────────────────────────────────────────
    @FXML private CheckBox foldLinesCheck;
    @FXML private CheckBox stitchMarksCheck;
    @FXML private CheckBox sewingHolesCheck;
    @FXML private CheckBox trimLinesCheck;
    @FXML private ComboBox<FolioStyle> bodyFolioStyleCombo;
    @FXML private ComboBox<FolioStyle> frontMatterFolioStyleCombo;
    @FXML private ComboBox<FolioStyle> rearMatterFolioStyleCombo;
    @FXML private Spinner<Integer> frontStartNumberSpinner;
    @FXML private Spinner<Integer> startNumberSpinner;
    @FXML private ComboBox<FolioPosition> folioPositionCombo;
    @FXML private CheckBox suppressFirstFolioCheck;
    @FXML private TableView<SignatureRow> signaturesTable;
    @FXML private TableColumn<SignatureRow, Number> colSigIndex;
    @FXML private TableColumn<SignatureRow, Number> colPageCount;
    @FXML private TableColumn<SignatureRow, Number> colSheetCount;
    @FXML private TableColumn<SignatureRow, String> colCreep;
    @FXML private TextField outputPathField;
    @FXML private Button exportButton;
    @FXML private Label exportResultLabel;

    // ── State ────────────────────────────────────────────────────────────────
    private final WizardState state = new WizardState();
    private int currentStep = 0;
    private ToggleGroup modeToggleGroup;
    private ToggleGroup paddingToggleGroup;
    private final Set<Integer> collapsedSignatures = new HashSet<>();
    private final Set<String> collapsedZones = new HashSet<>();
    private final Map<Integer, Image> thumbnailCache = new HashMap<>();
    private final Map<Integer, String> pageLabels = new HashMap<>();
    private Task<Map<Integer, Image>> activeThumbnailTask;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mode toggle (step 1)
        modeToggleGroup = new ToggleGroup();
        singlePdfToggle.setToggleGroup(modeToggleGroup);
        batchToggle.setToggleGroup(modeToggleGroup);
        singlePdfToggle.setSelected(true);
        modeToggleGroup.selectedToggleProperty().addListener(
            (obs, oldT, newT) -> onModeToggleChanged(newT));

        // Padding position toggle (step 2, group C only)
        paddingToggleGroup = new ToggleGroup();
        padAfterToggle.setToggleGroup(paddingToggleGroup);
        padBeforeToggle.setToggleGroup(paddingToggleGroup);
        padAfterToggle.setSelected(true);
        paddingToggleGroup.selectedToggleProperty().addListener(
            (obs, oldT, newT) -> onPaddingToggleChanged(newT));

        // Technique combo
        techniqueCombo.setItems(FXCollections.observableArrayList(BindingTechnique.values()));
        techniqueCombo.setValue(BindingTechnique.SADDLE_STITCH);
        techniqueCombo.setOnAction(e -> onTechniqueChanged());

        // Paper size
        paperSizeCombo.setItems(FXCollections.observableArrayList(PaperSize.values()));
        paperSizeCombo.setValue(PaperSize.A4);

        // Signature size spinner
        sigSizeSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4));
        sigSizeSpinner.setEditable(true);
        sigSizeSpinner.valueProperty().addListener((obs, o, n) -> updateOverflow());

        // Reading direction
        directionCombo.setItems(FXCollections.observableArrayList(ReadingDirection.values()));
        directionCombo.setValue(ReadingDirection.LTR);

        // Front/rear matter spinners (step 3, step by 4)
        frontMatterSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0, 4));
        frontMatterSpinner.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                onFrontMatterChanged(n);
            }
        });
        rearMatterSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0, 4));
        rearMatterSpinner.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                onRearMatterChanged(n);
            }
        });
        thumbnailCheck.selectedProperty().addListener(
            (obs, o, n) -> onThumbnailToggle(n));
        pageListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, n) -> updateSidePanel(n));

        // Numbering controls (step 4)
        bodyFolioStyleCombo.setItems(FXCollections.observableArrayList(FolioStyle.values()));
        bodyFolioStyleCombo.setValue(FolioStyle.ARABIC);
        frontMatterFolioStyleCombo.setItems(FXCollections.observableArrayList(FolioStyle.values()));
        frontMatterFolioStyleCombo.setValue(FolioStyle.NONE);
        rearMatterFolioStyleCombo.setItems(FXCollections.observableArrayList(FolioStyle.values()));
        rearMatterFolioStyleCombo.setValue(FolioStyle.NONE);
        frontStartNumberSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        frontStartNumberSpinner.setEditable(true);
        startNumberSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        startNumberSpinner.setEditable(true);
        folioPositionCombo.setItems(FXCollections.observableArrayList(FolioPosition.values()));
        folioPositionCombo.setValue(FolioPosition.BOTTOM_OUTER);

        // Page list with collapsable signature headers
        pageListView.setCellFactory(lv -> new PageListCell(this::toggleSignatureCollapse));

        // Imposition table columns
        colSigIndex.setCellValueFactory(r -> new SimpleIntegerProperty(r.getValue().index()));
        colPageCount.setCellValueFactory(
            r -> new SimpleIntegerProperty(r.getValue().pageCount()));
        colSheetCount.setCellValueFactory(
            r -> new SimpleIntegerProperty(r.getValue().sheetCount()));
        colCreep.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().creepSummary()));

        refreshDiagram();
        onTechniqueChanged();
        showStep(0);
    }

    // ── Menu actions ─────────────────────────────────────────────────────────

    @FXML
    private void handleMenuNewProject() {
        state.reset();
        collapsedSignatures.clear();
        pdfPathField.clear();
        pageCountLabel.setText("");
        quirePathField.clear();
        jobCountLabel.setText("");
        outputPathField.clear();
        exportResultLabel.setText("");
        pageListView.getItems().clear();
        frontMatterSpinner.getValueFactory().setValue(0);
        rearMatterSpinner.getValueFactory().setValue(0);
        thumbnailCheck.setSelected(false);
        thumbnailCache.clear();
        pageLabels.clear();
        collapsedZones.clear();
        pageLabelField.setText("");
        bodyFolioStyleCombo.setValue(FolioStyle.ARABIC);
        frontMatterFolioStyleCombo.setValue(FolioStyle.NONE);
        rearMatterFolioStyleCombo.setValue(FolioStyle.NONE);
        frontStartNumberSpinner.getValueFactory().setValue(1);
        startNumberSpinner.getValueFactory().setValue(1);
        folioPositionCombo.setValue(FolioPosition.BOTTOM_OUTER);
        suppressFirstFolioCheck.setSelected(false);
        showStep(0);
        setStatus("New project.");
    }

    @FXML
    private void handleMenuOpenPdf() {
        File f = pdfChooser("Open PDF").showOpenDialog(wizardStack.getScene().getWindow());
        if (f != null) {
            loadPdf(f.toPath());
            singlePdfToggle.setSelected(true);
            showStep(0);
        }
    }

    @FXML
    private void handleMenuExit() {
        Platform.exit();
    }

    @FXML
    private void handleMenuGuides() {
        openGuidesPanel();
    }

    @FXML
    private void handleAbout() {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle("About QuireBind");
        a.setHeaderText("QuireBind 1.0.0-SNAPSHOT");
        a.setContentText(
            "FOSS desktop application for preparing PDF files for bookbinding.\n"
            + "Licensed under the GNU Affero General Public License v3.0.");
        a.showAndWait();
    }

    // ── Step 1 actions ────────────────────────────────────────────────────────

    private void onModeToggleChanged(Toggle selected) {
        if (selected == null) {
            modeToggleGroup.selectToggle(singlePdfToggle);
            return;
        }
        boolean isSingle = selected == singlePdfToggle;
        state.setMode(isSingle ? WizardMode.SINGLE_PDF : WizardMode.BATCH);
        showNode(singlePdfPane, isSingle);
        showNode(batchPane, !isSingle);
    }

    @FXML
    private void handleBrowseInput() {
        File f = pdfChooser("Select Source PDF").showOpenDialog(
            wizardStack.getScene().getWindow());
        if (f != null) {
            loadPdf(f.toPath());
        }
    }

    @FXML
    private void handleBrowseQuire() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select .quire Batch File");
        fc.getExtensionFilters().add(new ExtensionFilter("Quire files", "*.quire"));
        File f = fc.showOpenDialog(wizardStack.getScene().getWindow());
        if (f != null) {
            loadQuireFile(f.toPath());
        }
    }

    private void loadPdf(Path path) {
        try {
            PageSequence seq = PdfPageLoader.load(path);
            state.setInputPdf(path);
            state.setPageCount(seq.pageCount());
            state.setPageSequence(seq);
            pdfPathField.setText(path.toString());
            pageCountLabel.setText("Pages loaded: " + seq.pageCount());
            setStatus("Loaded " + path.getFileName() + " — " + seq.pageCount() + " pages.");
        } catch (IOException e) {
            showError("Failed to load PDF", e.getMessage());
        }
    }

    private void loadQuireFile(Path path) {
        try {
            var config = QuireFileParser.parse(path);
            state.setBatchConfigPath(path);
            state.setBatchConfig(config);
            quirePathField.setText(path.toString());
            int jobCount = config.jobs() != null ? config.jobs().size() : 0;
            jobCountLabel.setText("Jobs loaded: " + jobCount);
            setStatus("Loaded batch config: " + path.getFileName()
                + " (" + jobCount + " job(s)).");
        } catch (IOException e) {
            showError("Failed to load .quire file", e.getMessage());
        }
    }

    // ── Step 2 actions ────────────────────────────────────────────────────────

    private void onTechniqueChanged() {
        BindingTechnique t = techniqueCombo.getValue();
        if (t == null) {
            return;
        }
        ImpositionGroup group = BindingGroupMapper.groupFor(t);
        boolean isGroupC = group == ImpositionGroup.C;
        showNode(sigSizeSpinner, isGroupC);
        showNode(sigSizeLabel, isGroupC);
        showNode(paddingPositionPane, isGroupC);
        refreshDiagram();
    }

    private void onPaddingToggleChanged(Toggle selected) {
        if (selected == null) {
            paddingToggleGroup.selectToggle(padAfterToggle);
            return;
        }
        state.setPaddingPosition(
            selected == padAfterToggle ? PaddingPosition.AFTER : PaddingPosition.BEFORE);
    }

    private void refreshDiagram() {
        BindingTechnique t = techniqueCombo != null && techniqueCombo.getValue() != null
            ? techniqueCombo.getValue() : BindingTechnique.SADDLE_STITCH;
        Canvas canvas = BindingTechniqueDiagram.forTechnique(t);
        if (diagramPane != null) {
            diagramPane.getChildren().setAll(canvas);
        }
    }

    // ── Step 3 actions ────────────────────────────────────────────────────────

    /**
     * Removes all existing COMPLETION_BLANK pages from the sequence, then re-inserts
     * the exact number needed to fill the last signature at the chosen position.
     */
    private void applyDefaultPadding() {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        ImpositionGroup group = BindingGroupMapper.groupFor(state.getTechnique());
        if (group != ImpositionGroup.C) {
            return;
        }
        int pps = state.getPagesPerSignature() * 4;

        // Strip old completion blanks so we don't double-count
        List<QuirePage> pages = new ArrayList<>(seq.getPages());
        pages.removeIf(p -> p.getPageType() == PageType.COMPLETION_BLANK);
        PageSequence clean = new PageSequence(pages);
        state.setPageSequence(clean);

        int total = clean.pageCount();
        int remainder = total % pps;
        if (remainder == 0) {
            return;
        }
        int blanksNeeded = pps - remainder;

        for (int i = 0; i < blanksNeeded; i++) {
            QuirePage blank = QuirePage.builder()
                .physicalPosition(0)
                .pageType(PageType.COMPLETION_BLANK)
                .build();
            if (state.getPaddingPosition() == PaddingPosition.BEFORE) {
                clean.insertPage(0, blank);
            } else {
                clean.insertPage(clean.pageCount(), blank);
            }
        }
        clean.reindex();
    }

    @FXML
    private void handleSetPageLabel() {
        PageItem item = pageListView.getSelectionModel().getSelectedItem();
        if (item == null || item.isHeader()) {
            return;
        }
        String label = pageLabelField.getText().trim();
        if (label.isEmpty()) {
            pageLabels.remove(item.seqIndex());
        } else {
            pageLabels.put(item.seqIndex(), label);
        }
        rebuildPageList();
        reselectBySeqIndex(item.seqIndex());
    }

    @FXML
    private void handleAddDecorativeFront() {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        QuirePage dec = QuirePage.builder()
            .physicalPosition(0)
            .pageType(PageType.AESTHETIC)
            .build();
        seq.insertPage(0, dec);
        seq.reindex();
        rebuildPageList();
        pageListView.getSelectionModel().select(0);
        setStatus("Decorative page added at front.");
    }

    @FXML
    private void handleAddDecorativeRear() {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        QuirePage dec = QuirePage.builder()
            .physicalPosition(0)
            .pageType(PageType.AESTHETIC)
            .build();
        seq.insertPage(seq.pageCount(), dec);
        seq.reindex();
        rebuildPageList();
        pageListView.getSelectionModel().selectLast();
        setStatus("Decorative page added at rear.");
    }

    @FXML
    private void handleAddBlankBefore() {
        int listIdx = pageListView.getSelectionModel().getSelectedIndex();
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        int seqPos;
        if (listIdx < 0) {
            seqPos = 0;
        } else {
            PageItem selected = pageListView.getItems().get(listIdx);
            seqPos = selected.isHeader() ? firstSeqIndexOfSig(selected.sigNum()) : selected.seqIndex();
        }
        insertBlank(seqPos);
    }

    @FXML
    private void handleAddBlankAfter() {
        int listIdx = pageListView.getSelectionModel().getSelectedIndex();
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        int seqPos;
        if (listIdx < 0) {
            seqPos = seq.pageCount();
        } else {
            PageItem selected = pageListView.getItems().get(listIdx);
            seqPos = selected.isHeader()
                ? lastSeqIndexOfSig(selected.sigNum()) + 1
                : selected.seqIndex() + 1;
        }
        insertBlank(seqPos);
    }

    private void insertBlank(int seqPos) {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(seqPos, seq.pageCount()));
        QuirePage blank = QuirePage.builder()
            .physicalPosition(clamped)
            .pageType(PageType.COMPLETION_BLANK)
            .build();
        seq.insertPage(clamped, blank);
        seq.reindex();
        rebuildPageList();
        // Re-select the newly inserted blank
        for (int i = 0; i < pageListView.getItems().size(); i++) {
            PageItem pi = pageListView.getItems().get(i);
            if (!pi.isHeader() && pi.seqIndex() == clamped) {
                pageListView.getSelectionModel().select(i);
                pageListView.scrollTo(i);
                break;
            }
        }
        setStatus("Blank inserted at position " + (clamped + 1) + ".");
    }

    @FXML
    private void handleRemovePage() {
        int listIdx = pageListView.getSelectionModel().getSelectedIndex();
        PageSequence seq = state.getPageSequence();
        if (listIdx < 0 || seq == null) {
            return;
        }
        PageItem item = pageListView.getItems().get(listIdx);
        if (item.isHeader()) {
            return;
        }
        seq.removePage(item.seqIndex());
        seq.reindex();
        rebuildPageList();
        setStatus("Page removed.");
    }

    @FXML
    private void handleMovePageUp() {
        int listIdx = pageListView.getSelectionModel().getSelectedIndex();
        PageSequence seq = state.getPageSequence();
        if (listIdx < 0 || seq == null) {
            return;
        }
        PageItem item = pageListView.getItems().get(listIdx);
        if (item.isHeader() || item.seqIndex() == 0) {
            return;
        }
        int from = item.seqIndex();
        seq.movePage(from, from - 1);
        seq.reindex();
        rebuildPageList();
        reselectBySeqIndex(from - 1);
    }

    @FXML
    private void handleMovePageDown() {
        int listIdx = pageListView.getSelectionModel().getSelectedIndex();
        PageSequence seq = state.getPageSequence();
        if (listIdx < 0 || seq == null) {
            return;
        }
        PageItem item = pageListView.getItems().get(listIdx);
        if (item.isHeader() || item.seqIndex() >= seq.pageCount() - 1) {
            return;
        }
        int from = item.seqIndex();
        seq.movePage(from, from + 1);
        seq.reindex();
        rebuildPageList();
        reselectBySeqIndex(from + 1);
    }

    private void reselectBySeqIndex(int seqIndex) {
        for (int i = 0; i < pageListView.getItems().size(); i++) {
            PageItem pi = pageListView.getItems().get(i);
            if (!pi.isHeader() && pi.seqIndex() == seqIndex) {
                pageListView.getSelectionModel().select(i);
                pageListView.scrollTo(i);
                return;
            }
        }
    }

    private int firstSeqIndexOfSig(int sigNum) {
        for (PageItem pi : pageListView.getItems()) {
            if (!pi.isHeader() && pi.sigNum() == sigNum) {
                return pi.seqIndex();
            }
        }
        return 0;
    }

    private int lastSeqIndexOfSig(int sigNum) {
        int last = 0;
        for (PageItem pi : pageListView.getItems()) {
            if (!pi.isHeader() && pi.sigNum() == sigNum) {
                last = pi.seqIndex();
            }
        }
        return last;
    }

    private void toggleSignatureCollapse(int sigNum) {
        if (collapsedSignatures.contains(sigNum)) {
            collapsedSignatures.remove(sigNum);
        } else {
            collapsedSignatures.add(sigNum);
        }
        rebuildPageList();
    }

    private void toggleZoneCollapse(String zone) {
        if (collapsedZones.contains(zone)) {
            collapsedZones.remove(zone);
        } else {
            collapsedZones.add(zone);
        }
        rebuildPageList();
    }

    private void onFrontMatterChanged(int newCount) {
        state.setFrontMatterPageCount(newCount);
        syncLeadingAesthetic(newCount);
        rebuildPageList();
    }

    private void onRearMatterChanged(int newCount) {
        state.setRearMatterPageCount(newCount);
        syncTrailingAesthetic(newCount);
        rebuildPageList();
    }

    private void syncLeadingAesthetic(int target) {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        int current = 0;
        for (QuirePage p : seq.getPages()) {
            if (p.getPageType() == PageType.AESTHETIC) {
                current++;
            } else {
                break;
            }
        }
        int diff = target - current;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                seq.insertPage(0, QuirePage.builder()
                    .physicalPosition(0).pageType(PageType.AESTHETIC).build());
            }
        } else {
            for (int i = 0; i < -diff; i++) {
                if (!seq.getPages().isEmpty()
                        && seq.getPages().get(0).getPageType() == PageType.AESTHETIC) {
                    seq.removePage(0);
                }
            }
        }
        seq.reindex();
    }

    private void syncTrailingAesthetic(int target) {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        List<QuirePage> pages = seq.getPages();
        int current = 0;
        for (int i = pages.size() - 1; i >= 0; i--) {
            if (pages.get(i).getPageType() == PageType.AESTHETIC) {
                current++;
            } else {
                break;
            }
        }
        int diff = target - current;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                seq.insertPage(seq.pageCount(), QuirePage.builder()
                    .physicalPosition(0).pageType(PageType.AESTHETIC).build());
            }
        } else {
            for (int i = 0; i < -diff; i++) {
                int last = seq.pageCount() - 1;
                if (last >= 0 && seq.getPages().get(last).getPageType() == PageType.AESTHETIC) {
                    seq.removePage(last);
                }
            }
        }
        seq.reindex();
    }

    private void updateSidePanel(PageItem item) {
        if (item == null || item.isHeader()) {
            thumbnailView.setVisible(false);
            thumbnailView.setManaged(false);
            thumbnailPlaceholder.setText("Select a page");
            thumbnailPlaceholder.setVisible(true);
            thumbnailPlaceholder.setManaged(true);
            pageLabelField.setText("");
            return;
        }
        thumbnailPlaceholder.setVisible(false);
        thumbnailPlaceholder.setManaged(false);
        Image img = thumbnailCache.get(item.seqIndex());
        if (img != null) {
            thumbnailView.setImage(img);
            thumbnailView.setVisible(true);
            thumbnailView.setManaged(true);
        } else {
            thumbnailView.setVisible(false);
            thumbnailView.setManaged(false);
            thumbnailPlaceholder.setText("No thumbnail — enable above");
            thumbnailPlaceholder.setVisible(true);
            thumbnailPlaceholder.setManaged(true);
        }
        String label = pageLabels.getOrDefault(item.seqIndex(), "");
        pageLabelField.setText(label);
    }

    private void onThumbnailToggle(boolean enabled) {
        if (!enabled) {
            if (activeThumbnailTask != null) {
                activeThumbnailTask.cancel();
                activeThumbnailTask = null;
            }
            thumbnailCache.clear();
            rebuildPageList();
            updateSidePanel(pageListView.getSelectionModel().getSelectedItem());
            return;
        }
        Path pdf = state.getInputPdf();
        PageSequence seq = state.getPageSequence();
        if (pdf == null || seq == null) {
            thumbnailCheck.setSelected(false);
            return;
        }
        startThumbnailLoading(pdf, seq.pageCount());
    }

    private void startThumbnailLoading(Path pdf, int pageCount) {
        Task<Map<Integer, Image>> task = new Task<>() {
            @Override
            protected Map<Integer, Image> call() throws Exception {
                Map<Integer, Image> cache = new HashMap<>();
                try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    for (int i = 0; i < pageCount && !isCancelled(); i++) {
                        updateProgress(i, pageCount);
                        updateMessage("Page " + (i + 1) + " / " + pageCount);
                        BufferedImage bi = renderer.renderImageWithDPI(i, 72);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "png", baos);
                        cache.put(i, new Image(new ByteArrayInputStream(baos.toByteArray())));
                    }
                }
                return cache;
            }
        };
        activeThumbnailTask = task;

        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Loading thumbnails…");
        ProgressBar pb = new ProgressBar();
        pb.progressProperty().bind(task.progressProperty());
        pb.setPrefWidth(280);
        Label msg = new Label("Preparing…");
        msg.textProperty().bind(task.messageProperty());
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> {
            task.cancel();
            progressStage.close();
            thumbnailCheck.setSelected(false);
        });
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, msg, pb, cancelBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        progressStage.setScene(new javafx.scene.Scene(box));

        task.setOnSucceeded(e -> {
            thumbnailCache.putAll(task.getValue());
            progressStage.close();
            rebuildPageList();
            updateSidePanel(pageListView.getSelectionModel().getSelectedItem());
        });
        task.setOnFailed(e -> {
            progressStage.close();
            thumbnailCheck.setSelected(false);
            showError("Thumbnail loading failed",
                task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });
        task.setOnCancelled(e -> progressStage.close());

        progressStage.show();
        Thread t = new Thread(task, "thumbnail-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Rebuilds the page list, inserting expandable/collapsable signature-boundary headers
     * and pre-computing overflow/underflow for the current signature settings.
     */
    private void rebuildPageList() {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            pageListView.setItems(FXCollections.emptyObservableList());
            overflowLabel.setText("");
            previewSummaryLabel.setText("");
            return;
        }

        List<QuirePage> pages = seq.getPages();
        int total = pages.size();
        int frontCount = state.getFrontMatterPageCount();
        int rearCount = state.getRearMatterPageCount();
        int bodyStart = Math.min(frontCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearCount);

        ImpositionGroup group = BindingGroupMapper.groupFor(
            techniqueCombo.getValue() != null
                ? techniqueCombo.getValue() : BindingTechnique.SADDLE_STITCH);
        int pps = (group == ImpositionGroup.C && sigSizeSpinner.getValue() != null)
            ? sigSizeSpinner.getValue() * 4 : Math.max(1, bodyEnd - bodyStart);

        ObservableList<PageItem> items = FXCollections.observableArrayList();

        // Front matter zone
        if (frontCount > 0) {
            boolean collapsed = collapsedZones.contains("FRONT");
            String arrow = collapsed ? "▶" : "▼";
            int sheets = frontCount / 4;
            items.add(PageItem.zoneHeader("FRONT",
                arrow + " Front Matter  (" + frontCount + " pages · " + sheets + " sheet)"));
            if (!collapsed) {
                for (int i = 0; i < Math.min(frontCount, total); i++) {
                    items.add(PageItem.page(0, i, pageLabel(pages.get(i), i),
                        pages.get(i).getPageType()));
                }
            }
        }

        // Body signatures
        int currentSig = 0;
        for (int i = bodyStart; i < bodyEnd; i++) {
            int relPos = i - bodyStart;
            boolean isSigStart = (group == ImpositionGroup.C && relPos % pps == 0)
                || (relPos == 0);
            if (isSigStart) {
                currentSig++;
                boolean collapsed = collapsedSignatures.contains(currentSig);
                String arrow = collapsed ? "▶" : "▼";
                int sigPageCount = Math.min(pps, bodyEnd - i);
                String overflow = (group == ImpositionGroup.C && sigPageCount < pps)
                    ? "  ⚠ " + (pps - sigPageCount) + " short" : "";
                items.add(PageItem.header(currentSig,
                    arrow + " Signature " + currentSig
                    + "  (" + sigPageCount + "/" + pps + " pages)" + overflow));
            }
            if (!collapsedSignatures.contains(currentSig)) {
                items.add(PageItem.page(currentSig, i, pageLabel(pages.get(i), i),
                    pages.get(i).getPageType()));
            }
        }

        // Rear matter zone
        if (rearCount > 0 && bodyEnd <= total) {
            boolean collapsed = collapsedZones.contains("REAR");
            String arrow = collapsed ? "▶" : "▼";
            int sheets = rearCount / 4;
            items.add(PageItem.zoneHeader("REAR",
                arrow + " Rear Matter  (" + rearCount + " pages · " + sheets + " sheet)"));
            if (!collapsed) {
                for (int i = bodyEnd; i < total; i++) {
                    items.add(PageItem.page(0, i, pageLabel(pages.get(i), i),
                        pages.get(i).getPageType()));
                }
            }
        }

        pageListView.setItems(items);
        updateOverflow();
        refreshPreviewSummary(pages, group, pps);
    }

    private void updateOverflow() {
        if (overflowLabel == null) {
            return;
        }
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            overflowLabel.setText("");
            return;
        }
        ImpositionGroup group = techniqueCombo != null && techniqueCombo.getValue() != null
            ? BindingGroupMapper.groupFor(techniqueCombo.getValue()) : ImpositionGroup.B;
        if (group != ImpositionGroup.C) {
            overflowLabel.setText("");
            return;
        }
        int pps = sigSizeSpinner.getValue() != null ? sigSizeSpinner.getValue() * 4 : 16;
        int total = seq.pageCount();
        int remainder = total % pps;
        if (remainder == 0) {
            int sigs = total / pps;
            overflowLabel.setText(sigs + " complete signature(s) — no padding needed.");
        } else {
            int blanksNeeded = pps - remainder;
            overflowLabel.setText(
                "⚠  Last signature needs " + blanksNeeded
                + " more page(s) to be complete (" + remainder + "/" + pps + ").");
        }
    }

    private void refreshPreviewSummary(List<QuirePage> pages, ImpositionGroup group, int pps) {
        long blanks = pages.stream().filter(p -> p.getPageType() != PageType.CONTENT).count();
        int sigs = group == ImpositionGroup.C
            ? (int) Math.ceil((double) pages.size() / pps) : 1;
        previewSummaryLabel.setText(
            pages.size() + " pages total  ·  "
            + blanks + " blank  ·  "
            + (pages.size() - blanks) + " content  ·  "
            + sigs + " signature(s)");
    }

    private String pageLabel(QuirePage page, int idx) {
        String custom = pageLabels.get(idx);
        if (custom != null && !custom.isBlank()) {
            return "  " + (idx + 1) + ".  " + custom;
        }
        String type = switch (page.getPageType()) {
            case CONTENT -> "Content";
            case AESTHETIC -> "Decorative";
            case COMPLETION_BLANK -> "Completion blank";
            case FILLER_BLANK -> "Filler blank";
        };
        String logical = page.getLogicalPageNumber()
            .map(n -> "  p." + n)
            .orElse("");
        return "  " + (idx + 1) + ".  " + type + logical;
    }

    // ── Step 4 actions ────────────────────────────────────────────────────────

    private void runImpositionAndPopulateTable() {
        PageSequence seq = state.getPageSequence();
        if (seq == null) {
            return;
        }
        double thickness = parseThickness();
        CreepConfig creepConfig = thickness > 0
            ? CreepConfig.builder().paperThicknessMm(thickness).build()
            : CreepConfig.builder().build();

        int sigSize = sigSizeSpinner.getValue() != null ? sigSizeSpinner.getValue() : 4;

        // Blanks are already in the sequence from step 3 — use zero completion padding
        QuireProject project = QuireProject.builder()
            .name(state.getInputPdf() != null
                ? state.getInputPdf().getFileName().toString() : "project")
            .bindingTechnique(state.getTechnique())
            .paperSize(state.getPaperSize())
            .readingDirection(state.getReadingDirection())
            .layout(ImpositionLayout.FOLIO)
            .pageSequence(seq)
            .paddingConfig(PaddingConfig.builder()
                .signatureSize(sigSize)
                .completionFront(0)
                .completionRear(0)
                .build())
            .numberingConfig(NumberingConfig.builder()
                .frontMatterStyle(state.getFrontMatterFolioStyle())
                .bodyStyle(state.getBodyFolioStyle())
                .rearMatterStyle(state.getRearMatterFolioStyle())
                .frontMatterStartNumber(state.getFrontMatterStartNumber())
                .bodyStartNumber(state.getBodyStartNumber())
                .suppressFirstBodyFolio(state.isSuppressFirstFolio())
                .folioPosition(state.getFolioPosition())
                .build())
            .markConfig(MarkConfig.builder().build())
            .creepConfig(creepConfig)
            .build();

        List<Signature> sigs = ImpositionEngine.impose(project);
        state.setImpositionResult(sigs);

        signaturesTable.setItems(FXCollections.observableArrayList(
            sigs.stream().map(SignatureRow::from).toList()));

        int totalSheets = sigs.stream().mapToInt(s -> s.getSheets().size()).sum();
        setStatus("Imposition: " + sigs.size() + " signature(s), " + totalSheets + " sheet(s).");
    }

    @FXML
    private void handleBrowseOutput() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Imposed PDF As");
        fc.getExtensionFilters().add(new ExtensionFilter("PDF Files", "*.pdf"));
        File f = fc.showSaveDialog(wizardStack.getScene().getWindow());
        if (f != null) {
            state.setOutputPdf(f.toPath());
            outputPathField.setText(f.toString());
        }
    }

    @FXML
    private void handleExport() {
        if (state.getOutputPdf() == null) {
            showError("No output path", "Please choose an output file first.");
            return;
        }
        if (!state.hasImpositionResult()) {
            showError("Not imposed", "Imposition has not been computed yet.");
            return;
        }
        collectMarksState();
        try {
            PdfImpositionWriter.write(
                state.getImpositionResult(),
                state.getInputPdf(),
                state.getOutputPdf(),
                state.getPaperSize());
            exportResultLabel.setText("Exported: " + state.getOutputPdf().getFileName());
            exportButton.setDisable(true);
            setStatus("Export complete.");
        } catch (IOException e) {
            showError("Export failed", e.getMessage());
        }
    }

    @FXML
    private void handleSaveTemplate() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save as .quire Template");
        fc.getExtensionFilters().add(new ExtensionFilter("Quire batch files", "*.quire"));
        File f = fc.showSaveDialog(wizardStack.getScene().getWindow());
        if (f == null) {
            return;
        }
        collectOptionsState();
        collectMarksState();
        try {
            QuireTemplateWriter.write(state, f.toPath());
            setStatus("Template saved: " + f.getName());
        } catch (IOException e) {
            showError("Failed to save template", e.getMessage());
        }
    }

    // ── Wizard navigation ─────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (currentStep > 0) {
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep >= TOTAL_STEPS - 1) {
            handleMenuNewProject();
            return;
        }
        if (!validateCurrentStep()) {
            return;
        }
        int next = currentStep + 1;
        if (next == 2) {
            collectOptionsState();
            collapsedSignatures.clear();
            applyDefaultPadding();
            rebuildPageList();
        }
        if (next == 3) {
            collectOptionsState();
            exportResultLabel.setText("");
            try {
                runImpositionAndPopulateTable();
            } catch (RuntimeException ex) {
                System.err.println("[QuireBind] Imposition error: " + ex.getMessage());
                ex.printStackTrace(System.err);
                showError("Imposition failed", ex.getMessage());
            }
            exportButton.setDisable(false);
        }
        showStep(next);
    }

    private boolean validateCurrentStep() {
        return switch (currentStep) {
            case 0 -> {
                if (state.getMode() == WizardMode.SINGLE_PDF && !state.hasInputPdf()) {
                    showError("No PDF selected", "Please select a source PDF.");
                    yield false;
                }
                if (state.getMode() == WizardMode.BATCH && !state.hasBatchConfig()) {
                    showError("No batch file", "Please select a .quire batch file.");
                    yield false;
                }
                yield true;
            }
            case 1 -> {
                collectOptionsState();
                yield true;
            }
            case 2 -> {
                if (state.getPageSequence() == null
                        || state.getPageSequence().pageCount() == 0) {
                    showError("Empty sequence", "There are no pages to impose.");
                    yield false;
                }
                yield true;
            }
            default -> true;
        };
    }

    private void collectOptionsState() {
        if (techniqueCombo.getValue() != null) {
            state.setTechnique(techniqueCombo.getValue());
        }
        if (paperSizeCombo.getValue() != null) {
            state.setPaperSize(paperSizeCombo.getValue());
        }
        if (sigSizeSpinner.getValue() != null) {
            state.setPagesPerSignature(sigSizeSpinner.getValue());
        }
        if (directionCombo.getValue() != null) {
            state.setReadingDirection(directionCombo.getValue());
        }
        state.setPaperThicknessMm(parseThickness());
    }

    private void collectMarksState() {
        state.setFoldLines(foldLinesCheck.isSelected());
        state.setStitchMarks(stitchMarksCheck.isSelected());
        state.setSewingHoles(sewingHolesCheck.isSelected());
        state.setTrimLines(trimLinesCheck.isSelected());
        if (bodyFolioStyleCombo.getValue() != null) {
            state.setBodyFolioStyle(bodyFolioStyleCombo.getValue());
        }
        if (frontMatterFolioStyleCombo.getValue() != null) {
            state.setFrontMatterFolioStyle(frontMatterFolioStyleCombo.getValue());
        }
        if (rearMatterFolioStyleCombo.getValue() != null) {
            state.setRearMatterFolioStyle(rearMatterFolioStyleCombo.getValue());
        }
        if (frontStartNumberSpinner.getValue() != null) {
            state.setFrontMatterStartNumber(frontStartNumberSpinner.getValue());
        }
        if (startNumberSpinner.getValue() != null) {
            state.setBodyStartNumber(startNumberSpinner.getValue());
        }
        if (folioPositionCombo.getValue() != null) {
            state.setFolioPosition(folioPositionCombo.getValue());
        }
        state.setSuppressFirstFolio(suppressFirstFolioCheck.isSelected());
    }

    private void showStep(int step) {
        currentStep = step;
        List<VBox> steps = List.of(stepLoad, stepOptions, stepPreview, stepExport);
        for (int i = 0; i < steps.size(); i++) {
            showNode(steps.get(i), i == step);
        }
        stepIndicatorLabel.setText("Step " + (step + 1) + " of " + TOTAL_STEPS);
        backButton.setDisable(step == 0);
        boolean isLast = step == TOTAL_STEPS - 1;
        nextButton.setText(isLast ? "↺ New Project" : "Next →");
        nextButton.setDisable(false);
    }

    // ── Guides panel ──────────────────────────────────────────────────────────

    private void openGuidesPanel() {
        try {
            URL fxml = getClass().getResource(
                "/com/maiitsoh/quirebind/desktop/fxml/guides-panel.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            Scene scene = new Scene(loader.load(), 760, 500);
            URL css = getClass().getResource(
                "/com/maiitsoh/quirebind/desktop/css/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            Stage stage = new Stage();
            stage.setTitle("QuireBind — Binding Guides");
            stage.initModality(Modality.NONE);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Could not open guides", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double parseThickness() {
        String text = thicknessField.getText().trim();
        if (text.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private FileChooser pdfChooser(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new ExtensionFilter("PDF Files", "*.pdf"));
        return fc;
    }

    private void showError(String header, String body) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle("QuireBind");
        a.setHeaderText(header);
        a.setContentText(body);
        a.showAndWait();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private static void showNode(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Represents one row in the page list.
     * {@code zone} is non-null only for zone headers ("FRONT", "REAR").
     */
    public record PageItem(boolean isHeader, boolean isZoneHeader, String zone,
                           int sigNum, int seqIndex, String label, PageType pageType) {

        /** Creates a front/rear matter zone header row. */
        static PageItem zoneHeader(String zone, String label) {
            return new PageItem(true, true, zone, 0, -1, label, null);
        }

        /** Creates a signature boundary header row. */
        static PageItem header(int sigNum, String label) {
            return new PageItem(true, false, null, sigNum, -1, label, null);
        }

        /** Creates a page row. */
        static PageItem page(int sigNum, int seqIndex, String label, PageType pageType) {
            return new PageItem(false, false, null, sigNum, seqIndex, label, pageType);
        }
    }

    /** Cell that styles headers and page rows distinctly, and toggles collapse on click. */
    private final class PageListCell extends ListCell<PageItem> {

        private final Consumer<Integer> onSigToggle;

        PageListCell(Consumer<Integer> onSigToggle) {
            this.onSigToggle = onSigToggle;
        }

        @Override
        protected void updateItem(PageItem item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("sig-header-cell", "blank-page-cell",
                "content-page-cell", "overflow-sig-cell", "zone-header-cell");
            setOnMouseClicked(null);
            if (empty || item == null) {
                setText(null);
            } else if (item.isZoneHeader()) {
                setText(item.label());
                getStyleClass().add("zone-header-cell");
                setOnMouseClicked(e -> toggleZoneCollapse(item.zone()));
            } else if (item.isHeader()) {
                setText(item.label());
                getStyleClass().add(item.label().contains("⚠")
                    ? "overflow-sig-cell" : "sig-header-cell");
                setOnMouseClicked(e -> onSigToggle.accept(item.sigNum()));
            } else {
                setText(item.label());
                boolean isBlank = item.pageType() != PageType.CONTENT;
                getStyleClass().add(isBlank ? "blank-page-cell" : "content-page-cell");
            }
        }
    }

    /**
     * Flat summary row for the imposition result table on the export step.
     */
    public record SignatureRow(int index, int pageCount, int sheetCount, String creepSummary) {

        static SignatureRow from(Signature sig) {
            double maxCreep = sig.getSheets().stream()
                .mapToDouble(s -> s.getCreepResult().map(r -> r.getCreepMm()).orElse(0.0))
                .max().orElse(0.0);
            String creep = maxCreep > 0
                ? String.format("%.3f mm", maxCreep) : "—";
            return new SignatureRow(
                sig.getSignatureIndex() + 1,
                sig.getLogicalPageNumbers().size(),
                sig.getSheets().size(),
                creep);
        }
    }
}
