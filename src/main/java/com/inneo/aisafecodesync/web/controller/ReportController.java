package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.core.report.SyncReport;
import com.inneo.aisafecodesync.web.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/runs")
    public String history(Model model) {
        model.addAttribute("runs", reportService.history());
        return "runs/history";
    }

    @GetMapping("/reports/{id}")
    public String details(@PathVariable long id, Model model) {
        SyncReport report = reportService.privateReport(id);
        model.addAttribute("report", report);
        model.addAttribute("profileId", reportService.profileIdForRun(id));
        return "reports/details";
    }

    @GetMapping("/reports/{id}/export.json")
    public ResponseEntity<String> json(@PathVariable long id) {
        return download("ai-safe-report-" + id + ".json", MediaType.APPLICATION_JSON, reportService.aiSafeJson(id));
    }

    @GetMapping("/reports/{id}/export.csv")
    public ResponseEntity<String> csv(@PathVariable long id) {
        return download("ai-safe-report-" + id + ".csv", MediaType.valueOf("text/csv"), reportService.aiSafeCsv(id));
    }

    private ResponseEntity<String> download(String filename, MediaType mediaType, String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType(mediaType, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
