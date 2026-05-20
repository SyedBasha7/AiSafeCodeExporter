package com.inneo.aisafecodesync.web.service;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.report.CsvReportWriter;
import com.inneo.aisafecodesync.core.report.JsonReportWriter;
import com.inneo.aisafecodesync.core.report.ReportSanitizer;
import com.inneo.aisafecodesync.core.report.SyncReport;
import com.inneo.aisafecodesync.core.scan.LeakScanner;
import com.inneo.aisafecodesync.exception.AiSafeCodeSyncException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.inneo.aisafecodesync.persistence.repository.SyncRunRepository;
import com.inneo.aisafecodesync.web.dto.RedactionRuleSnapshot;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.inneo.aisafecodesync.web.mapper.RunReportMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReportService {

    private static final TypeReference<List<RedactionRuleSnapshot>> REDACTION_SNAPSHOT_LIST = new TypeReference<>() {
    };

    private final SyncRunRepository runRepository;
    private final RunReportMapper runReportMapper;
    private final ProfileMapper profileMapper;
    private final ReportSanitizer reportSanitizer;
    private final JsonReportWriter jsonReportWriter;
    private final CsvReportWriter csvReportWriter;
    private final LeakScanner leakScanner;
    private final ObjectMapper objectMapper;

    public ReportService(
            SyncRunRepository runRepository,
            RunReportMapper runReportMapper,
            ProfileMapper profileMapper,
            ReportSanitizer reportSanitizer,
            JsonReportWriter jsonReportWriter,
            CsvReportWriter csvReportWriter,
            LeakScanner leakScanner,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.runReportMapper = runReportMapper;
        this.profileMapper = profileMapper;
        this.reportSanitizer = reportSanitizer;
        this.jsonReportWriter = jsonReportWriter;
        this.csvReportWriter = csvReportWriter;
        this.leakScanner = leakScanner;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Transactional(readOnly = true)
    public List<SyncRunEntity> history() {
        return runRepository.findTop50ByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public SyncReport privateReport(long runId) {
        return runReportMapper.toReport(getRun(runId));
    }

    @Transactional(readOnly = true)
    public Long profileIdForRun(long runId) {
        SyncRunEntity run = getRun(runId);
        SyncProfileEntity profile = run.getProfile();
        return profile == null ? null : profile.getId();
    }

    @Transactional(readOnly = true)
    public String aiSafeJson(long runId) {
        SyncRunEntity run = getRun(runId);
        SyncConfig config = configForRun(run);
        SyncReport report = reportSanitizer.sanitizeForAiExport(runReportMapper.toReport(run), config);
        String json = jsonReportWriter.write(report);
        verifyAiSafeExport(json, config);
        return json;
    }

    @Transactional(readOnly = true)
    public String aiSafeCsv(long runId) {
        SyncRunEntity run = getRun(runId);
        SyncConfig config = configForRun(run);
        SyncReport report = reportSanitizer.sanitizeForAiExport(runReportMapper.toReport(run), config);
        String csv = csvReportWriter.write(report);
        verifyAiSafeExport(csv, config);
        return csv;
    }

    private SyncRunEntity getRun(long runId) {
        return runRepository.findById(runId).orElseThrow(() -> new NoSuchElementException("Run not found: " + runId));
    }

    private SyncConfig configForRun(SyncRunEntity run) {
        List<SensitiveTermRule> redactionRules = redactionRulesForRun(run);
        return new SyncConfig(
                run.getProfileName(),
                run.getProfileType() == null ? ProfileType.AI_SAFE_EXPORT : run.getProfileType(),
                run.getSourceRoot() == null || run.getSourceRoot().isBlank() ? null : Path.of(run.getSourceRoot()),
                run.getTargetRoot() == null || run.getTargetRoot().isBlank() ? null : Path.of(run.getTargetRoot()),
                List.of(),
                List.of(),
                List.of(),
                redactionRules,
                false,
                StandardCharsets.UTF_8
        );
    }

    private List<SensitiveTermRule> redactionRulesForRun(SyncRunEntity run) {
        List<RedactionRuleSnapshot> snapshots = readSnapshots(run);
        if (snapshots.isEmpty() && run.getProfile() != null) {
            SyncConfig currentProfileConfig = profileMapper.toConfig(run.getProfile());
            snapshots = currentProfileConfig.replacementRules().stream()
                    .filter(rule -> !rule.search().isBlank())
                    .map(rule -> new RedactionRuleSnapshot("replacement:" + rule.id(), List.of(rule.search()), rule.caseSensitive()))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            currentProfileConfig.sensitiveTermRules().stream()
                    .filter(rule -> !rule.values().isEmpty())
                    .map(rule -> new RedactionRuleSnapshot("sensitive:" + rule.id(), rule.values(), rule.caseSensitive()))
                    .forEach(snapshots::add);
        }
        return snapshots.stream()
                .filter(snapshot -> !snapshot.values().isEmpty())
                .map(snapshot -> new SensitiveTermRule(snapshot.id(), snapshot.values(), snapshot.caseSensitive(), true))
                .toList();
    }

    private List<RedactionRuleSnapshot> readSnapshots(SyncRunEntity run) {
        if (run.getRedactionValuesJson() == null || run.getRedactionValuesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(run.getRedactionValuesJson(), REDACTION_SNAPSHOT_LIST);
        } catch (JsonProcessingException ex) {
            throw new AiSafeCodeSyncException("Run redaction snapshot could not be parsed. Refusing to export an AI-safe report.", ex);
        }
    }

    private void verifyAiSafeExport(String content, SyncConfig config) {
        if (!leakScanner.scanReportContent(content, config.sensitiveTermRules()).isEmpty()
                || containsRoot(content, config.sourceRoot())
                || containsRoot(content, config.targetRoot())) {
            throw new AiSafeCodeSyncException("AI-safe report verification failed. Refusing to export content that still contains configured private values.");
        }
    }

    private boolean containsRoot(String content, Path root) {
        if (content == null || root == null) {
            return false;
        }
        String normalizedRoot = root.toAbsolutePath().normalize().toString();
        String alternate = normalizedRoot.contains("\\") ? normalizedRoot.replace('\\', '/') : normalizedRoot.replace('/', '\\');
        return content.contains(normalizedRoot) || content.contains(alternate);
    }
}
