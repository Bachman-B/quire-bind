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
import com.maiitsoh.quirebind.batch.runner.BatchTemplateRunner;
import com.maiitsoh.quirebind.core.binding.BindingGroupMapper;
import com.maiitsoh.quirebind.core.imposition.FolioAssigner;
import com.maiitsoh.quirebind.core.imposition.PagePaddingApplier;
import com.maiitsoh.quirebind.core.imposition.SignatureComposer;
import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.maiitsoh.quirebind.core.model.ImposedSheet;
import com.maiitsoh.quirebind.core.model.ImpositionGroup;
import com.maiitsoh.quirebind.core.model.ImpositionLayout;
import com.maiitsoh.quirebind.core.model.MarkConfig;
import com.maiitsoh.quirebind.core.model.SewingConfig;
import com.maiitsoh.quirebind.core.model.NumberingConfig;
import com.maiitsoh.quirebind.core.model.PaddingConfig;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.Signature;
import com.maiitsoh.quirebind.core.pdf.PdfImpositionWriter;
import com.maiitsoh.quirebind.core.pdf.PdfPageLoader;
import com.maiitsoh.quirebind.desktop.diagram.BindingTechniqueDiagram;
import com.maiitsoh.quirebind.desktop.state.WizardState;
import com.maiitsoh.quirebind.desktop.state.WizardState.BatchOutputMode;
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
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.HBox;
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
import java.util.LinkedHashMap;
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
    @FXML private ListView<String> sourcePdfListView;
    @FXML private Label pageCountLabel;
    @FXML private TextField quirePathField;
    @FXML private Label jobCountLabel;
    @FXML private ListView<String> batchPdfListView;
    @FXML private ToggleButton batchSuffixToggle;
    @FXML private ToggleButton batchFolderToggle;
    @FXML private HBox batchSuffixPane;
    @FXML private HBox batchFolderPane;
    @FXML private TextField batchSuffixField;
    @FXML private TextField batchOutputDirField;

    // ── Batch run step ────────────────────────────────────────────────────────
    @FXML private VBox stepBatchRun;
    @FXML private Button runBatchButton;
    @FXML private ProgressBar batchProgressBar;
    @FXML private Label batchProgressLabel;
    @FXML private ListView<String> batchResultListView;
    @FXML private Label batchResultSummaryLabel;

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
    @FXML private VBox sewingHolesParams;
    @FXML private ToggleButton sewingSimpleToggle;
    @FXML private ToggleButton sewingBandedToggle;
    @FXML private HBox sewingSimpleParams;
    @FXML private HBox sewingBandedParams;
    @FXML private Spinner<Integer> sewingHoleCountSpinner;
    @FXML private Spinner<Integer> sewingBandCountSpinner;
    @FXML private TextField sewingBandWidthField;
    @FXML private TextField sewingEndMarginField;
    @FXML private CheckBox trimLinesCheck;
    @FXML private ComboBox<FolioStyle> bodyFolioStyleCombo;
    @FXML private ComboBox<FolioStyle> frontMatterFolioStyleCombo;
    @FXML private ComboBox<FolioStyle> rearMatterFolioStyleCombo;
    @FXML private Spinner<Integer> frontStartNumberSpinner;
    @FXML private Spinner<Integer> startNumberSpinner;
    @FXML private Spinner<Integer> rearStartNumberSpinner;
    @FXML private ComboBox<FolioPosition> folioPositionCombo;
    @FXML private CheckBox suppressFirstFolioCheck;
    @FXML private TableView<SignatureRow> signaturesTable;
    @FXML private TableColumn<SignatureRow, String> colZone;
    @FXML private TableColumn<SignatureRow, Number> colSigIndex;
    @FXML private TableColumn<SignatureRow, Number> colPageCount;
    @FXML private TableColumn<SignatureRow, Number> colSheetCount;
    @FXML private TableColumn<SignatureRow, String> colCreep;
    @FXML private TextField outputPathField;
    @FXML private Button exportButton;
    @FXML private Label exportResultLabel;

    // ── State ────────────────────────────────────────────────────────────────
    private static final int STEP_BATCH_RUN = 4;

    private final WizardState state = new WizardState();
    private int currentStep = 0;
    private ToggleGroup modeToggleGroup;
    private ToggleGroup paddingToggleGroup;
    private ToggleGroup sewingStyleToggleGroup;
    private ToggleGroup batchOutputToggleGroup;
    private final Set<Integer> expandedSignatures = new HashSet<>();
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
        rearStartNumberSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        rearStartNumberSpinner.setEditable(true);
        folioPositionCombo.setItems(FXCollections.observableArrayList(FolioPosition.values()));
        folioPositionCombo.setValue(FolioPosition.BOTTOM_OUTER);

        // Sewing hole sub-controls
        sewingHoleCountSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 9, 5));
        sewingHoleCountSpinner.setEditable(true);
        sewingBandCountSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 3));
        sewingBandCountSpinner.setEditable(true);
        sewingBandWidthField.setText("10");
        sewingEndMarginField.setText("15");
        sewingHolesCheck.selectedProperty().addListener(
            (obs, o, n) -> sewingHolesParams.setDisable(!n));
        sewingStyleToggleGroup = new ToggleGroup();
        sewingSimpleToggle.setToggleGroup(sewingStyleToggleGroup);
        sewingBandedToggle.setToggleGroup(sewingStyleToggleGroup);
        sewingSimpleToggle.setSelected(true);
        sewingStyleToggleGroup.selectedToggleProperty().addListener(
            (obs, oldT, newT) -> onSewingStyleToggleChanged(newT));

        // Batch output mode toggle
        batchOutputToggleGroup = new ToggleGroup();
        batchSuffixToggle.setToggleGroup(batchOutputToggleGroup);
        batchFolderToggle.setToggleGroup(batchOutputToggleGroup);
        batchSuffixToggle.setSelected(true);
        batchOutputToggleGroup.selectedToggleProperty().addListener(
            (obs, oldT, newT) -> onBatchOutputToggleChanged(newT));

        // Keep state in sync as PDFs are added/removed
        batchPdfListView.getItems().addListener(
            (ListChangeListener<String>) c ->
                updateBatchNextButton());

        // Page list with collapsable signature headers
        pageListView.setCellFactory(lv -> new PageListCell(this::toggleSignatureCollapse));

        // Imposition table columns
        colZone.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().zone()));
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
        expandedSignatures.clear();
        sourcePdfListView.getItems().clear();
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
        rearStartNumberSpinner.getValueFactory().setValue(1);
        folioPositionCombo.setValue(FolioPosition.BOTTOM_OUTER);
        suppressFirstFolioCheck.setSelected(false);
        sewingStyleToggleGroup.selectToggle(sewingSimpleToggle);
        sewingHoleCountSpinner.getValueFactory().setValue(5);
        sewingBandCountSpinner.getValueFactory().setValue(3);
        sewingBandWidthField.setText("10");
        sewingEndMarginField.setText("15");
        sewingHolesParams.setDisable(true);
        batchPdfListView.getItems().clear();
        batchSuffixField.setText("-imposed");
        batchOutputDirField.clear();
        batchOutputToggleGroup.selectToggle(batchSuffixToggle);
        batchResultListView.getItems().clear();
        batchResultSummaryLabel.setText("");
        showStep(0);
        setStatus("New project.");
    }

    @FXML
    private void handleMenuOpenPdf() {
        List<File> files = pdfChooser("Open PDF")
            .showOpenMultipleDialog(wizardStack.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            state.reset();
            sourcePdfListView.getItems().clear();
            pageCountLabel.setText("");
            thumbnailCache.clear();
            pageLabels.clear();
            for (File f : files) {
                addInputPdf(f.toPath());
            }
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
        FileChooser fc = pdfChooser("Add Source PDF");
        fc.setTitle("Add PDF files");
        List<File> files = fc.showOpenMultipleDialog(wizardStack.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                addInputPdf(f.toPath());
            }
        }
    }

    @FXML
    private void handleRemoveSourcePdf() {
        int idx = sourcePdfListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            return;
        }
        state.removeInputPdf(idx);
        sourcePdfListView.getItems().remove(idx);
        rebuildSequenceFromSources();
    }

    @FXML
    private void handleMoveSourceUp() {
        int idx = sourcePdfListView.getSelectionModel().getSelectedIndex();
        if (idx <= 0) {
            return;
        }
        state.moveInputPdfUp(idx);
        String item = sourcePdfListView.getItems().remove(idx);
        sourcePdfListView.getItems().add(idx - 1, item);
        sourcePdfListView.getSelectionModel().select(idx - 1);
        rebuildSequenceFromSources();
    }

    @FXML
    private void handleMoveSourceDown() {
        int idx = sourcePdfListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= sourcePdfListView.getItems().size() - 1) {
            return;
        }
        state.moveInputPdfDown(idx);
        String item = sourcePdfListView.getItems().remove(idx);
        sourcePdfListView.getItems().add(idx + 1, item);
        sourcePdfListView.getSelectionModel().select(idx + 1);
        rebuildSequenceFromSources();
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

    private void addInputPdf(Path path) {
        if (state.getInputPdfs().contains(path)) {
            return;
        }
        state.addInputPdf(path);
        try (var doc = org.apache.pdfbox.Loader.loadPDF(path.toFile())) {
            int pages = doc.getNumberOfPages();
            sourcePdfListView.getItems().add(path.getFileName() + " (" + pages + " pages)");
        } catch (IOException e) {
            state.removeInputPdf(state.getInputPdfs().size() - 1);
            showError("Failed to open PDF", e.getMessage());
            return;
        }
        rebuildSequenceFromSources();
    }

    private void rebuildSequenceFromSources() {
        try {
            if (state.getInputPdfs().isEmpty()) {
                state.setPageSequence(null);
                pageCountLabel.setText("");
                setStatus("No source PDFs loaded.");
                return;
            }
            PageSequence seq = PdfPageLoader.loadAll(state.getInputPdfs());
            state.setPageSequence(seq);
            int total = seq.pageCount();
            int sources = state.getInputPdfs().size();
            pageCountLabel.setText(
                sources + " file" + (sources != 1 ? "s" : "")
                + " — " + total + " page" + (total != 1 ? "s" : "") + " total");
            setStatus("Loaded " + sources + " source" + (sources != 1 ? "s" : "")
                + ", " + total + " pages.");
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
            String technique = config.defaults() != null
                ? config.defaults().technique() : "unknown";
            jobCountLabel.setText("Technique: " + technique);
            setStatus("Loaded template: " + path.getFileName() + " (" + technique + ").");
        } catch (IOException e) {
            showError("Failed to load .quire file", e.getMessage());
        }
    }

    @FXML
    private void handleBatchAddPdfs() {
        FileChooser fc = pdfChooser("Add PDF files");
        fc.setTitle("Add PDF files for batch processing");
        List<File> files = fc.showOpenMultipleDialog(wizardStack.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                Path p = f.toPath();
                if (!state.getBatchInputPdfs().contains(p)) {
                    state.getBatchInputPdfs().add(p);
                    batchPdfListView.getItems().add(f.toString());
                }
            }
        }
    }

    @FXML
    private void handleBatchRemovePdf() {
        int idx = batchPdfListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            state.getBatchInputPdfs().remove(idx);
            batchPdfListView.getItems().remove(idx);
        }
    }

    @FXML
    private void handleBatchBrowseOutputDir() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Select output folder");
        File dir = dc.showDialog(wizardStack.getScene().getWindow());
        if (dir != null) {
            state.setBatchOutputDir(dir.toPath());
            batchOutputDirField.setText(dir.toString());
        }
    }

    private void onBatchOutputToggleChanged(Toggle selected) {
        if (selected == null) {
            batchOutputToggleGroup.selectToggle(batchSuffixToggle);
            return;
        }
        boolean isSuffix = selected == batchSuffixToggle;
        showNode(batchSuffixPane, isSuffix);
        showNode(batchFolderPane, !isSuffix);
        state.setBatchOutputMode(isSuffix ? BatchOutputMode.SUFFIX : BatchOutputMode.OUTPUT_DIR);
    }

    private void updateBatchNextButton() {
        if (state.getMode() == WizardMode.BATCH && currentStep == 0) {
            nextButton.setDisable(false);
        }
    }

    @FXML
    private void handleRunBatch() {
        if (state.getBatchConfig() == null) {
            showError("No template", "Please go back and select a .quire template.");
            return;
        }
        if (state.getBatchInputPdfs().isEmpty()) {
            showError("No input files", "Please go back and add at least one PDF.");
            return;
        }
        if (state.getBatchOutputMode() == BatchOutputMode.OUTPUT_DIR
                && state.getBatchOutputDir() == null) {
            showError("No output folder", "Please go back and select an output folder.");
            return;
        }
        runBatchButton.setDisable(true);
        batchResultListView.getItems().clear();
        batchResultSummaryLabel.setText("");
        showNode(batchProgressBar, true);
        showNode(batchProgressLabel, true);
        batchProgressBar.setProgress(-1);
        batchProgressLabel.setText("Running…");

        List<Path> inputs = List.copyOf(state.getBatchInputPdfs());
        var template = state.getBatchConfig();
        var outputMode = state.getBatchOutputMode();
        var suffix = batchSuffixField.getText().trim().isEmpty()
                ? "-imposed" : batchSuffixField.getText().trim();
        var outputDir = state.getBatchOutputDir();

        Task<List<BatchTemplateRunner.TemplateJobResult>> task = new Task<>() {
            @Override
            protected List<BatchTemplateRunner.TemplateJobResult> call() {
                if (outputMode == BatchOutputMode.OUTPUT_DIR) {
                    return BatchTemplateRunner.runWithDirectory(template, inputs, outputDir, false);
                } else {
                    return BatchTemplateRunner.runWithSuffix(template, inputs, suffix, false);
                }
            }
        };
        task.setOnSucceeded(e -> {
            List<BatchTemplateRunner.TemplateJobResult> results = task.getValue();
            long ok = results.stream().filter(BatchTemplateRunner.TemplateJobResult::success).count();
            long fail = results.size() - ok;
            for (BatchTemplateRunner.TemplateJobResult r : results) {
                String icon = r.success() ? "✓" : "✗";
                batchResultListView.getItems().add(
                    icon + "  " + r.inputPath().getFileName() + "  →  " + r.message());
            }
            batchResultSummaryLabel.setText(
                "Done: " + ok + " succeeded, " + fail + " failed.");
            showNode(batchProgressBar, false);
            showNode(batchProgressLabel, false);
            runBatchButton.setDisable(false);
            setStatus("Batch complete: " + ok + "/" + results.size() + " succeeded.");
        });
        task.setOnFailed(e -> {
            showNode(batchProgressBar, false);
            showNode(batchProgressLabel, false);
            runBatchButton.setDisable(false);
            showError("Batch failed", task.getException() != null
                ? task.getException().getMessage() : "Unknown error");
        });
        Thread t = new Thread(task, "batch-runner");
        t.setDaemon(true);
        t.start();
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

    private void onSewingStyleToggleChanged(Toggle selected) {
        if (selected == null) {
            sewingStyleToggleGroup.selectToggle(sewingSimpleToggle);
            return;
        }
        boolean isBanded = selected == sewingBandedToggle;
        showNode(sewingSimpleParams, !isBanded);
        showNode(sewingBandedParams, isBanded);
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

        int frontCount = state.getFrontMatterPageCount();
        int rearCount = state.getRearMatterPageCount();
        int total = clean.pageCount();
        int bodyStart = Math.min(frontCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearCount);
        int bodyCount = bodyEnd - bodyStart;

        int remainder = bodyCount % pps;
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
                clean.insertPage(bodyStart, blank);
                bodyStart++;
                bodyEnd++;
            } else {
                clean.insertPage(bodyEnd, blank);
                bodyEnd++;
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
        shiftThumbnailCacheUp(0);
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
        shiftThumbnailCacheUp(clamped);
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
        int removedIdx = item.seqIndex();
        seq.removePage(removedIdx);
        seq.reindex();
        shiftThumbnailCacheDown(removedIdx);
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
        swapThumbnailCache(from, from - 1);
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
        swapThumbnailCache(from, from + 1);
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

    private void shiftThumbnailCacheUp(int fromSeqIndex) {
        Map<Integer, Image> shifted = new HashMap<>();
        for (Map.Entry<Integer, Image> entry : thumbnailCache.entrySet()) {
            int k = entry.getKey();
            shifted.put(k >= fromSeqIndex ? k + 1 : k, entry.getValue());
        }
        thumbnailCache.clear();
        thumbnailCache.putAll(shifted);
    }

    private void shiftThumbnailCacheDown(int removedSeqIndex) {
        Map<Integer, Image> shifted = new HashMap<>();
        for (Map.Entry<Integer, Image> entry : thumbnailCache.entrySet()) {
            int k = entry.getKey();
            if (k == removedSeqIndex) {
                continue;
            }
            shifted.put(k > removedSeqIndex ? k - 1 : k, entry.getValue());
        }
        thumbnailCache.clear();
        thumbnailCache.putAll(shifted);
    }

    private void swapThumbnailCache(int a, int b) {
        Image imgA = thumbnailCache.get(a);
        Image imgB = thumbnailCache.get(b);
        if (imgA != null) {
            thumbnailCache.put(b, imgA);
        } else {
            thumbnailCache.remove(b);
        }
        if (imgB != null) {
            thumbnailCache.put(a, imgB);
        } else {
            thumbnailCache.remove(a);
        }
    }

    private void toggleSignatureCollapse(int sigNum) {
        if (expandedSignatures.contains(sigNum)) {
            expandedSignatures.remove(sigNum);
        } else {
            expandedSignatures.add(sigNum);
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
        if (item == null) {
            thumbnailView.setVisible(false);
            thumbnailView.setManaged(false);
            thumbnailPlaceholder.setText("Select a page");
            thumbnailPlaceholder.setVisible(true);
            thumbnailPlaceholder.setManaged(true);
            pageLabelField.setText("");
            return;
        }
        if (item.isHeader()) {
            // Don't disturb the current display when a header row is clicked
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
        Map<String, Path> sourcePaths = state.getSourceDocPaths();
        PageSequence seq = state.getPageSequence();
        if (sourcePaths.isEmpty() || seq == null) {
            thumbnailCheck.setSelected(false);
            return;
        }
        startThumbnailLoading(sourcePaths, seq);
    }

    private void startThumbnailLoading(Map<String, Path> sourcePaths, PageSequence seq) {
        Task<Map<Integer, Image>> task = new Task<>() {
            @Override
            protected Map<Integer, Image> call() throws Exception {
                Map<Integer, Image> cache = new HashMap<>();
                List<QuirePage> seqPages = seq.getPages();
                int total = seqPages.size();
                Map<String, PDDocument> openDocs = new LinkedHashMap<>();
                Map<String, PDFRenderer> renderers = new LinkedHashMap<>();
                try {
                    for (Map.Entry<String, Path> entry : sourcePaths.entrySet()) {
                        PDDocument doc = Loader.loadPDF(entry.getValue().toFile());
                        openDocs.put(entry.getKey(), doc);
                        renderers.put(entry.getKey(), new PDFRenderer(doc));
                    }
                    for (int seqIdx = 0; seqIdx < total && !isCancelled(); seqIdx++) {
                        updateProgress(seqIdx, total);
                        updateMessage("Page " + (seqIdx + 1) + " / " + total);
                        QuirePage page = seqPages.get(seqIdx);
                        if (page.getSourcePageIndex().isEmpty()) {
                            continue;
                        }
                        String docId = page.getSourceDocumentId().orElse(null);
                        PDFRenderer renderer = docId != null ? renderers.get(docId) : null;
                        if (renderer == null) {
                            continue;
                        }
                        int pdfIdx = page.getSourcePageIndex().get();
                        BufferedImage bi = renderer.renderImageWithDPI(pdfIdx, 72);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "png", baos);
                        cache.put(seqIdx, new Image(new ByteArrayInputStream(baos.toByteArray())));
                    }
                } finally {
                    for (PDDocument doc : openDocs.values()) {
                        try {
                            doc.close();
                        } catch (IOException ignored) {
                            // best-effort
                        }
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
                boolean collapsed = !expandedSignatures.contains(currentSig);
                String arrow = collapsed ? "▶" : "▼";
                int sigPageCount = Math.min(pps, bodyEnd - i);
                String overflow = (group == ImpositionGroup.C && sigPageCount < pps)
                    ? "  ⚠ " + (pps - sigPageCount) + " short" : "";
                items.add(PageItem.header(currentSig,
                    arrow + " Signature " + currentSig
                    + "  (" + sigPageCount + "/" + pps + " pages)" + overflow));
            }
            if (expandedSignatures.contains(currentSig)) {
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
        refreshPreviewSummary(pages, group, pps, bodyEnd - bodyStart);
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
        int total = seq.pageCount() - state.getFrontMatterPageCount() - state.getRearMatterPageCount();
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

    private void refreshPreviewSummary(List<QuirePage> pages, ImpositionGroup group, int pps,
            int bodyCount) {
        long blanks = pages.stream().filter(p -> p.getPageType() != PageType.CONTENT).count();
        int sigs = group == ImpositionGroup.C
            ? (int) Math.ceil((double) bodyCount / pps) : 1;
        previewSummaryLabel.setText(
            pages.size() + " pages total  ·  "
            + blanks + " blank  ·  "
            + (pages.size() - blanks) + " content  ·  "
            + sigs + " body signature(s)");
    }

    private String pageLabel(QuirePage page, int idx) {
        String custom = pageLabels.get(idx);
        if (custom != null && !custom.isBlank()) {
            return "  " + (idx + 1) + ".  " + custom;
        }
        String type = switch (page.getPageType()) {
            case CONTENT -> page.getSourceDocumentId()
                .map(id -> {
                    String fname = java.nio.file.Path.of(id).getFileName().toString();
                    return fname + page.getSourcePageIndex()
                        .map(i -> " p." + (i + 1)).orElse("");
                })
                .orElse("Content");
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
        int sigSize = sigSizeSpinner.getValue() != null ? sigSizeSpinner.getValue() : 4;
        ImpositionGroup group = BindingGroupMapper.groupFor(state.getTechnique());

        PaddingConfig paddingConfig = PaddingConfig.builder()
            .signatureSize(sigSize)
            .completionFront(0)
            .completionRear(0)
            .build();

        NumberingConfig numberingConfig = NumberingConfig.builder()
            .frontMatterStyle(state.getFrontMatterFolioStyle())
            .bodyStyle(state.getBodyFolioStyle())
            .rearMatterStyle(state.getRearMatterFolioStyle())
            .frontMatterStartNumber(state.getFrontMatterStartNumber())
            .bodyStartNumber(state.getBodyStartNumber())
            .rearMatterStartNumber(state.getRearMatterStartNumber())
            .suppressFirstBodyFolio(state.isSuppressFirstFolio())
            .folioPosition(state.getFolioPosition())
            .build();

        ZonedResult zonedResult = imposeWithZones(seq, group, paddingConfig, numberingConfig,
            state.getReadingDirection());
        state.setImpositionResult(zonedResult.allSignatures());

        List<SignatureRow> rows = new ArrayList<>();
        List<Signature> all = zonedResult.allSignatures();
        int frontEnd = zonedResult.frontSigCount();
        int bodyEnd = frontEnd + zonedResult.bodySigCount();
        for (int i = 0; i < all.size(); i++) {
            String zone = i < frontEnd ? "Front Matter"
                : i < bodyEnd ? "Body" : "Rear Matter";
            rows.add(SignatureRow.from(all.get(i), zone));
        }
        signaturesTable.setItems(FXCollections.observableArrayList(rows));

        int totalSheets = all.stream().mapToInt(s -> s.getSheets().size()).sum();
        setStatus("Imposition: " + all.size() + " signature(s), " + totalSheets + " sheet(s).");
    }

    private ZonedResult imposeWithZones(
            PageSequence seq,
            ImpositionGroup group,
            PaddingConfig paddingConfig,
            NumberingConfig numberingConfig,
            ReadingDirection direction) {
        int frontCount = state.getFrontMatterPageCount();
        int rearCount = state.getRearMatterPageCount();
        int total = seq.pageCount();
        int bodyStart = Math.min(frontCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearCount);

        List<QuirePage> frontPages = new ArrayList<>(seq.getPages().subList(0, bodyStart));
        List<QuirePage> bodyRaw = new ArrayList<>(seq.getPages().subList(bodyStart, bodyEnd));
        List<QuirePage> rearPages = new ArrayList<>(seq.getPages().subList(bodyEnd, total));

        // Apply filler-blank padding to body only; front/rear are always multiples of 4
        List<QuirePage> bodyPadded = PagePaddingApplier.pad(bodyRaw, paddingConfig, group);

        // Assign folios across the full sequence so zone detection works correctly
        List<QuirePage> combined = new ArrayList<>(frontPages.size() + bodyPadded.size() + rearPages.size());
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
                all.add(reindexSignature(s, offset++));
            }
        }
        int frontSigCount = all.size();

        int bodySigCount = 0;
        if (!nBody.isEmpty()) {
            for (Signature s : SignatureComposer.compose(
                    nBody, group, ImpositionLayout.FOLIO,
                    paddingConfig.getSignatureSize(), direction)) {
                all.add(reindexSignature(s, offset++));
                bodySigCount++;
            }
        }

        if (!nRear.isEmpty()) {
            for (Signature s : SignatureComposer.compose(
                    nRear, ImpositionGroup.B, ImpositionLayout.FOLIO, 1, direction)) {
                all.add(reindexSignature(s, offset++));
            }
        }

        return new ZonedResult(List.copyOf(all), frontSigCount, bodySigCount);
    }

    private static Signature reindexSignature(Signature sig, int newIndex) {
        List<ImposedSheet> newSheets = sig.getSheets().stream()
            .map(sheet -> ImposedSheet.builder()
                .sheetIndex(sheet.getSheetIndex())
                .signatureIndex(newIndex)
                .frontPages(sheet.getFrontPages())
                .backPages(sheet.getBackPages())
                .build())
            .toList();
        return Signature.builder()
            .signatureIndex(newIndex)
            .sheets(newSheets)
            .logicalPageNumbers(sig.getLogicalPageNumbers())
            .build();
    }

    @FXML
    private void handleBrowseOutput() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Imposed PDF As");
        fc.getExtensionFilters().add(new ExtensionFilter("PDF Files", "*.pdf"));
        File f = fc.showSaveDialog(wizardStack.getScene().getWindow());
        if (f != null) {
            if (!f.getName().endsWith(".pdf")) {
                f = new File(f.getParentFile(), f.getName() + ".pdf");
            }
            state.setOutputPdf(f.toPath());
            outputPathField.setText(f.toString());
            exportButton.setDisable(false);
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
        SewingConfig sewingConfig = state.isSewingHoles()
            ? SewingConfig.builder()
                .style(state.getSewingStyle())
                .holeCount(state.getSewingHoleCount())
                .endMarginMm(state.getSewingEndMarginMm())
                .bandCount(state.getSewingBandCount())
                .bandWidthMm(state.getSewingBandWidthMm())
                .build()
            : null;
        MarkConfig markConfig = MarkConfig.builder()
            .foldLines(state.isFoldLines())
            .signatureProofMarkers(state.isStitchMarks())
            .sewingHoles(state.isSewingHoles())
            .sewingConfig(sewingConfig)
            .trimLines(state.isTrimLines())
            .build();
        NumberingConfig numberingConfig = NumberingConfig.builder()
            .frontMatterStyle(state.getFrontMatterFolioStyle())
            .bodyStyle(state.getBodyFolioStyle())
            .rearMatterStyle(state.getRearMatterFolioStyle())
            .frontMatterStartNumber(state.getFrontMatterStartNumber())
            .bodyStartNumber(state.getBodyStartNumber())
            .rearMatterStartNumber(state.getRearMatterStartNumber())
            .suppressFirstBodyFolio(state.isSuppressFirstFolio())
            .folioPosition(state.getFolioPosition())
            .build();
        try {
            PdfImpositionWriter.write(
                state.getImpositionResult(),
                state.getSourceDocPaths(),
                state.getOutputPdf(),
                state.getPaperSize(),
                markConfig,
                numberingConfig);
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
        if (!f.getName().endsWith(".quire")) {
            f = new File(f.getParentFile(), f.getName() + ".quire");
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
        if (currentStep == STEP_BATCH_RUN) {
            showStep(0);
        } else if (currentStep > 0) {
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep == STEP_BATCH_RUN || currentStep >= TOTAL_STEPS - 1) {
            handleMenuNewProject();
            return;
        }
        if (!validateCurrentStep()) {
            return;
        }
        // In batch mode, step 0 jumps straight to the batch run panel
        if (currentStep == 0 && state.getMode() == WizardMode.BATCH) {
            state.setBatchOutputSuffix(batchSuffixField.getText().trim().isEmpty()
                ? "-imposed" : batchSuffixField.getText().trim());
            showStep(STEP_BATCH_RUN);
            return;
        }
        int next = currentStep + 1;
        if (next == 2) {
            collectOptionsState();
            expandedSignatures.clear();
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
                if (state.getMode() == WizardMode.BATCH) {
                    if (!state.hasBatchConfig()) {
                        showError("No template", "Please select a .quire template file.");
                        yield false;
                    }
                    if (state.getBatchInputPdfs().isEmpty()) {
                        showError("No input files", "Please add at least one PDF to process.");
                        yield false;
                    }
                    if (state.getBatchOutputMode() == BatchOutputMode.OUTPUT_DIR
                            && state.getBatchOutputDir() == null) {
                        showError("No output folder",
                            "Please select an output folder, or switch to suffix mode.");
                        yield false;
                    }
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
        state.setSewingStyle(sewingBandedToggle.isSelected()
            ? SewingConfig.SewingStyle.BANDED : SewingConfig.SewingStyle.SIMPLE);
        if (sewingHoleCountSpinner.getValue() != null) {
            state.setSewingHoleCount(sewingHoleCountSpinner.getValue());
        }
        if (sewingBandCountSpinner.getValue() != null) {
            state.setSewingBandCount(sewingBandCountSpinner.getValue());
        }
        try {
            double bw = Double.parseDouble(sewingBandWidthField.getText().trim());
            state.setSewingBandWidthMm(bw > 0 ? bw : 10.0);
        } catch (NumberFormatException ignored) {
            state.setSewingBandWidthMm(10.0);
        }
        try {
            double margin = Double.parseDouble(sewingEndMarginField.getText().trim());
            state.setSewingEndMarginMm(margin > 0 ? margin : 15.0);
        } catch (NumberFormatException ignored) {
            state.setSewingEndMarginMm(15.0);
        }
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
        if (rearStartNumberSpinner.getValue() != null) {
            state.setRearMatterStartNumber(rearStartNumberSpinner.getValue());
        }
        if (folioPositionCombo.getValue() != null) {
            state.setFolioPosition(folioPositionCombo.getValue());
        }
        state.setSuppressFirstFolio(suppressFirstFolioCheck.isSelected());
    }

    private void showStep(int step) {
        currentStep = step;
        List<VBox> steps = List.of(stepLoad, stepOptions, stepPreview, stepExport, stepBatchRun);
        for (int i = 0; i < steps.size(); i++) {
            showNode(steps.get(i), i == step);
        }
        if (step == STEP_BATCH_RUN) {
            stepIndicatorLabel.setText("Batch — Step 2 of 2");
            backButton.setDisable(false);
            nextButton.setText("↺ New Project");
            nextButton.setDisable(false);
        } else {
            stepIndicatorLabel.setText("Step " + (step + 1) + " of " + TOTAL_STEPS);
            backButton.setDisable(step == 0);
            boolean isLast = step == TOTAL_STEPS - 1;
            nextButton.setText(isLast ? "↺ New Project" : "Next →");
            nextButton.setDisable(false);
        }
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

    private record ZonedResult(List<Signature> allSignatures, int frontSigCount, int bodySigCount) {}

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
    public record SignatureRow(int index, int pageCount, int sheetCount, String creepSummary,
            String zone) {

        static SignatureRow from(Signature sig, String zone) {
            double maxCreep = sig.getSheets().stream()
                .mapToDouble(s -> s.getCreepResult().map(r -> r.getCreepMm()).orElse(0.0))
                .max().orElse(0.0);
            String creep = maxCreep > 0
                ? String.format("%.3f mm", maxCreep) : "—";
            return new SignatureRow(
                sig.getSignatureIndex() + 1,
                sig.getSheets().size() * 4,
                sig.getSheets().size(),
                creep,
                zone);
        }
    }
}
