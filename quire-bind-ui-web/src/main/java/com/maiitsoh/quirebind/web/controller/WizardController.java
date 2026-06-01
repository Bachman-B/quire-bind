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
package com.maiitsoh.quirebind.web.controller;

import com.maiitsoh.quirebind.core.binding.BindingGroupMapper;
import com.maiitsoh.quirebind.core.model.BindingTechnique;
import com.maiitsoh.quirebind.core.model.FolioPosition;
import com.maiitsoh.quirebind.core.model.FolioStyle;
import com.maiitsoh.quirebind.core.model.ImpositionGroup;
import com.maiitsoh.quirebind.core.model.PageSequence;
import com.maiitsoh.quirebind.core.model.PageType;
import com.maiitsoh.quirebind.core.model.PaperSize;
import com.maiitsoh.quirebind.core.model.QuirePage;
import com.maiitsoh.quirebind.core.model.ReadingDirection;
import com.maiitsoh.quirebind.core.model.SewingConfig;
import com.maiitsoh.quirebind.core.pdf.PdfPageLoader;
import org.apache.pdfbox.Loader;
import com.maiitsoh.quirebind.guides.loader.GuideLoader;
import com.maiitsoh.quirebind.guides.model.BindingGuide;
import com.maiitsoh.quirebind.guides.renderer.MarkdownToHtml;
import com.maiitsoh.quirebind.web.model.WebSession;
import com.maiitsoh.quirebind.web.service.WebImpositionService;
import com.maiitsoh.quirebind.web.service.WebImpositionService.SignatureSummary;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Handles all wizard steps and the guides panel for the web UI. */
@Controller
public class WizardController {

    private static final String STEP_UPLOAD = "fragments/upload :: step";
    private static final String STEP_BINDING = "fragments/binding :: step";
    private static final String STEP_PAGES = "fragments/pages :: step";
    private static final String STEP_EXPORT = "fragments/export :: step";

    private final WebImpositionService impositionService;
    private final WebSession session;

    /** Constructs the controller with the required imposition service and session bean. */
    public WizardController(WebImpositionService impositionService, WebSession session) {
        this.impositionService = impositionService;
        this.session = session;
    }

    /** Renders the main application shell with step 1 (upload) loaded. */
    @GetMapping("/")
    public String index(Model model) {
        addUploadModel(model);
        return "index";
    }

    /** Adds a PDF to the source list and re-renders the upload step. */
    @PostMapping("/wizard/upload")
    public String upload(
            @RequestParam("pdf") MultipartFile file,
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) throws IOException {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a PDF file.");
            addUploadModel(model);
            return htmx != null ? STEP_UPLOAD : "index";
        }
        Path tmp = Files.createTempFile("quire-upload-", ".pdf");
        file.transferTo(tmp);
        String name = file.getOriginalFilename();
        int count;
        try (var doc = Loader.loadPDF(tmp.toFile())) {
            count = doc.getNumberOfPages();
        }
        session.addSource(tmp, name != null ? name : "document.pdf", count);
        session.setPageSequence(rebuildSequence());
        session.setImpositionResult(null);
        addUploadModel(model);
        return htmx != null ? STEP_UPLOAD : "index";
    }

    /** Removes a source PDF by index and re-renders the upload step. */
    @PostMapping("/wizard/upload/remove")
    public String removeSource(
            @RequestParam("index") int index,
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) throws IOException {
        session.removeSource(index);
        session.setPageSequence(rebuildSequence());
        session.setImpositionResult(null);
        addUploadModel(model);
        return htmx != null ? STEP_UPLOAD : "index";
    }

    /** Validates that at least one source exists and advances to the binding step. */
    @PostMapping("/wizard/upload/continue")
    public String uploadContinue(
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) {
        if (!session.hasSources()) {
            model.addAttribute("error", "Please add at least one PDF file.");
            addUploadModel(model);
            return htmx != null ? STEP_UPLOAD : "index";
        }
        addBindingModel(model);
        return htmx != null ? STEP_BINDING : "index";
    }

    private PageSequence rebuildSequence() throws IOException {
        if (session.getSources().isEmpty()) {
            return null;
        }
        List<Path> paths = session.getSources().stream()
            .map(WebSession.SourceEntry::tempPath)
            .toList();
        return PdfPageLoader.loadAll(paths);
    }

    /** Saves binding configuration and advances to the pages step. */
    @PostMapping("/wizard/binding")
    public String binding(
            @RequestParam("technique") String techniqueStr,
            @RequestParam("paperSize") String paperSizeStr,
            @RequestParam("readingDirection") String dirStr,
            @RequestParam(value = "signatureSize", defaultValue = "4") int signatureSize,
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) {
        session.setTechnique(BindingTechnique.valueOf(techniqueStr));
        session.setPaperSize(PaperSize.valueOf(paperSizeStr));
        session.setReadingDirection(ReadingDirection.valueOf(dirStr));
        session.setSignatureSize(Math.max(1, signatureSize));
        session.setImpositionResult(null);
        addPagesModel(model);
        return htmx != null ? STEP_PAGES : "index";
    }

    /** Saves page-zone configuration and advances to the export step with imposition results. */
    @PostMapping("/wizard/pages")
    public String pages(
            @RequestParam(value = "frontMatterPages", defaultValue = "0") int frontPages,
            @RequestParam(value = "rearMatterPages", defaultValue = "0") int rearPages,
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) throws IOException {
        session.setFrontMatterPageCount(Math.max(0, frontPages));
        session.setRearMatterPageCount(Math.max(0, rearPages));
        impositionService.impose(session);
        List<SignatureSummary> summary = impositionService.summarise(session);
        addExportModel(model, summary, null);
        return htmx != null ? STEP_EXPORT : "index";
    }

    /** Goes back from the binding step to the upload step. */
    @PostMapping("/wizard/back/upload")
    public String backToUpload(
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) {
        addUploadModel(model);
        return htmx != null ? STEP_UPLOAD : "index";
    }

    /** Goes back from the pages step to the binding step. */
    @PostMapping("/wizard/back/binding")
    public String backToBinding(
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) {
        addBindingModel(model);
        return htmx != null ? STEP_BINDING : "index";
    }

    /** Goes back from the export step to the pages step. */
    @PostMapping("/wizard/back/pages")
    public String backToPages(
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Model model) {
        addPagesModel(model);
        return htmx != null ? STEP_PAGES : "index";
    }

    /** Streams the imposed PDF to the browser as a file download. */
    @PostMapping("/wizard/download")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(value = "foldLines", defaultValue = "false") boolean foldLines,
            @RequestParam(value = "stitchMarks", defaultValue = "false") boolean stitchMarks,
            @RequestParam(value = "sewingHoles", defaultValue = "false") boolean sewingHoles,
            @RequestParam(value = "sewingStyle", defaultValue = "SIMPLE") String sewingStyleStr,
            @RequestParam(value = "sewingHoleCount", defaultValue = "5") int sewingHoleCount,
            @RequestParam(value = "sewingEndMarginMm", defaultValue = "15.0") double sewingEndMarginMm,
            @RequestParam(value = "sewingBandCount", defaultValue = "3") int sewingBandCount,
            @RequestParam(value = "sewingBandWidthMm", defaultValue = "10.0") double sewingBandWidthMm,
            @RequestParam(value = "trimLines", defaultValue = "false") boolean trimLines,
            @RequestParam(value = "frontFolioStyle", defaultValue = "NONE") String frontStyleStr,
            @RequestParam(value = "frontMatterStartNumber", defaultValue = "1") int frontStart,
            @RequestParam(value = "bodyFolioStyle", defaultValue = "ARABIC") String bodyStyleStr,
            @RequestParam(value = "bodyStartNumber", defaultValue = "1") int bodyStart,
            @RequestParam(value = "rearFolioStyle", defaultValue = "NONE") String rearStyleStr,
            @RequestParam(value = "rearMatterStartNumber", defaultValue = "1") int rearStart,
            @RequestParam(value = "folioPosition", defaultValue = "BOTTOM_OUTER") String posStr,
            @RequestParam(value = "suppressFirstFolio", defaultValue = "false")
                boolean suppressFirst) {
        session.setFoldLines(foldLines);
        session.setStitchMarks(stitchMarks);
        session.setSewingHoles(sewingHoles);
        session.setSewingStyle(SewingConfig.SewingStyle.valueOf(sewingStyleStr));
        session.setSewingHoleCount(Math.max(2, sewingHoleCount));
        session.setSewingEndMarginMm(sewingEndMarginMm > 0 ? sewingEndMarginMm : 15.0);
        session.setSewingBandCount(Math.max(1, sewingBandCount));
        session.setSewingBandWidthMm(sewingBandWidthMm > 0 ? sewingBandWidthMm : 10.0);
        session.setTrimLines(trimLines);
        session.setFrontMatterFolioStyle(FolioStyle.valueOf(frontStyleStr));
        session.setFrontMatterStartNumber(Math.max(1, frontStart));
        session.setBodyFolioStyle(FolioStyle.valueOf(bodyStyleStr));
        session.setBodyStartNumber(Math.max(1, bodyStart));
        session.setRearMatterFolioStyle(FolioStyle.valueOf(rearStyleStr));
        session.setRearMatterStartNumber(Math.max(1, rearStart));
        session.setFolioPosition(FolioPosition.valueOf(posStr));
        session.setSuppressFirstFolio(suppressFirst);

        String filename = deriveOutputFilename(session.getOriginalFilename());
        StreamingResponseBody body = out -> impositionService.export(session, out);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString())
            .body(body);
    }

    /** Inserts a blank page at the given 0-based position in the sequence. */
    @PostMapping("/wizard/pages/insert-blank")
    public String insertBlank(
            @RequestParam("position") int position,
            Model model) {
        PageSequence seq = session.getPageSequence();
        if (seq != null) {
            int clamped = Math.max(0, Math.min(position, seq.pageCount()));
            seq.insertPage(clamped, QuirePage.builder()
                .physicalPosition(clamped)
                .pageType(PageType.COMPLETION_BLANK)
                .build());
            seq.reindex();
        }
        addPageListModel(model);
        return "fragments/page-list :: rows";
    }

    /** Removes the page at the given 0-based position from the sequence. */
    @PostMapping("/wizard/pages/remove")
    public String removePage(
            @RequestParam("position") int position,
            Model model) {
        PageSequence seq = session.getPageSequence();
        if (seq != null && position >= 0 && position < seq.pageCount()) {
            seq.removePage(position);
            seq.reindex();
        }
        addPageListModel(model);
        return "fragments/page-list :: rows";
    }

    /** Moves the page at the given 0-based position one step toward the front. */
    @PostMapping("/wizard/pages/move-up")
    public String movePageUp(
            @RequestParam("position") int position,
            Model model) {
        PageSequence seq = session.getPageSequence();
        if (seq != null && position > 0 && position < seq.pageCount()) {
            seq.movePage(position, position - 1);
            seq.reindex();
        }
        addPageListModel(model);
        return "fragments/page-list :: rows";
    }

    /** Moves the page at the given 0-based position one step toward the rear. */
    @PostMapping("/wizard/pages/move-down")
    public String movePageDown(
            @RequestParam("position") int position,
            Model model) {
        PageSequence seq = session.getPageSequence();
        if (seq != null && position >= 0 && position < seq.pageCount() - 1) {
            seq.movePage(position, position + 1);
            seq.reindex();
        }
        addPageListModel(model);
        return "fragments/page-list :: rows";
    }

    /** Returns the guides panel page showing the requested technique's guide. */
    @GetMapping("/guides")
    public String guides(
            @RequestParam(value = "technique", defaultValue = "saddle_stitch") String techniqueId,
            Model model) throws IOException {
        List<BindingGuide> allGuides = GuideLoader.loadAll();
        BindingGuide selected = allGuides.stream()
            .filter(g -> g.getMetadata().getTechniqueId().equals(techniqueId))
            .findFirst()
            .orElse(allGuides.isEmpty() ? null : allGuides.get(0));
        model.addAttribute("guides", allGuides);
        model.addAttribute("selected", selected);
        if (selected != null) {
            String dirId = selected.getMetadata().getTechniqueId().replace('_', '-');
            String html = MarkdownToHtml.toHtml(selected.getBody(),
                src -> resolveGuideImage(dirId, src));
            model.addAttribute("guideHtml", html);
        }
        return "guides";
    }

    private static String resolveGuideImage(String dirId, String src) {
        String resourcePath = "/guides/" + dirId + "/" + src;
        try (var in = WizardController.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            String mime = src.endsWith(".svg") ? "image/svg+xml" : "image/png";
            return "data:" + mime + ";base64,"
                + java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    private static String deriveOutputFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "imposed.pdf";
        }
        String base = originalFilename.endsWith(".pdf")
            ? originalFilename.substring(0, originalFilename.length() - 4)
            : originalFilename;
        return base + "-imposed.pdf";
    }

    private void addUploadModel(Model model) {
        model.addAttribute("step", 1);
        model.addAttribute("sources", session.getSources());
    }

    private void addBindingModel(Model model) {
        model.addAttribute("step", 2);
        model.addAttribute("techniques", Arrays.asList(BindingTechnique.values()));
        model.addAttribute("paperSizes", Arrays.asList(PaperSize.values()));
        model.addAttribute("ws", session);
    }

    private void addPagesModel(Model model) {
        model.addAttribute("step", 3);
        model.addAttribute("ws", session);
        addPageListModel(model);
    }

    private void addPageListModel(Model model) {
        PageSequence seq = session.getPageSequence();
        if (seq == null) {
            model.addAttribute("pageItems", List.of());
            model.addAttribute("pageCount", 0);
            return;
        }
        List<QuirePage> pages = seq.getPages();
        int total = pages.size();
        int frontCount = session.getFrontMatterPageCount();
        int rearCount = session.getRearMatterPageCount();
        int bodyStart = Math.min(frontCount, total);
        int bodyEnd = Math.max(bodyStart, total - rearCount);
        ImpositionGroup group = BindingGroupMapper.groupFor(session.getTechnique());
        int pps = group == ImpositionGroup.C
            ? session.getSignatureSize() * 4
            : Math.max(1, bodyEnd - bodyStart);

        List<PageRow> items = new ArrayList<>(total + 8);

        if (bodyStart > 0) {
            int sheets = bodyStart / 4;
            items.add(PageRow.header("Front Matter — " + bodyStart + " page"
                + (bodyStart != 1 ? "s" : "") + " · " + sheets + " sheet"
                + (sheets != 1 ? "s" : ""), "zone-front"));
            for (int i = 0; i < bodyStart; i++) {
                items.add(PageRow.page(pages.get(i), i));
            }
        }

        int sigNum = 0;
        for (int i = bodyStart; i < bodyEnd; i++) {
            int relPos = i - bodyStart;
            boolean isSigStart = group == ImpositionGroup.C
                ? relPos % pps == 0 : relPos == 0;
            if (isSigStart) {
                sigNum++;
                int sigPageCount = Math.min(pps, bodyEnd - i);
                String warning = (group == ImpositionGroup.C && sigPageCount < pps)
                    ? " ⚠ " + (pps - sigPageCount) + " short" : "";
                items.add(PageRow.header("Signature " + sigNum
                    + " — " + sigPageCount + "/" + pps + " pages" + warning, "zone-body"));
            }
            items.add(PageRow.page(pages.get(i), i));
        }

        if (bodyEnd < total) {
            int rearActual = total - bodyEnd;
            int sheets = rearActual / 4;
            items.add(PageRow.header("Rear Matter — " + rearActual + " page"
                + (rearActual != 1 ? "s" : "") + " · " + sheets + " sheet"
                + (sheets != 1 ? "s" : ""), "zone-rear"));
            for (int i = bodyEnd; i < total; i++) {
                items.add(PageRow.page(pages.get(i), i));
            }
        }

        model.addAttribute("pageItems", items);
        model.addAttribute("pageCount", total);
    }

    /** Display model for a row in the page list — either a section header or a page entry. */
    record PageRow(int position, String label, String cssClass, boolean header) {

        static PageRow header(String label, String cssClass) {
            return new PageRow(-1, label, cssClass, true);
        }

        static PageRow page(QuirePage p, int idx) {
            String label = switch (p.getPageType()) {
                case CONTENT -> "Content";
                case AESTHETIC -> "Decorative";
                case COMPLETION_BLANK, FILLER_BLANK -> "Blank";
            };
            String css = switch (p.getPageType()) {
                case CONTENT -> "content";
                case AESTHETIC -> "aesthetic";
                case COMPLETION_BLANK, FILLER_BLANK -> "blank";
            };
            return new PageRow(idx, label, css, false);
        }
    }

    private void addExportModel(Model model, List<SignatureSummary> summary, String error) {
        model.addAttribute("step", 4);
        model.addAttribute("ws", session);
        model.addAttribute("summary", summary);
        model.addAttribute("folioStyles", Arrays.asList(FolioStyle.values()));
        model.addAttribute("folioPositions", Arrays.asList(FolioPosition.values()));
        if (error != null) {
            model.addAttribute("error", error);
        }
    }
}
