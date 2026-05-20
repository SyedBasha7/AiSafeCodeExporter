package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.report.CsvReportWriter;
import com.inneo.aisafecodesync.core.report.JsonReportWriter;
import com.inneo.aisafecodesync.core.report.ReportSanitizer;
import com.inneo.aisafecodesync.core.report.SyncReportEntry;
import com.inneo.aisafecodesync.core.scan.LeakScanner;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncReportEntryEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.inneo.aisafecodesync.persistence.repository.SyncRunRepository;
import com.inneo.aisafecodesync.web.dto.RedactionRuleSnapshot;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.inneo.aisafecodesync.web.mapper.RunReportMapper;
import com.inneo.aisafecodesync.web.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RunReportMapper runReportMapper = new RunReportMapper(objectMapper);

    @Test
    void aiSafeExportUsesRunRedactionSnapshotWhenProfileChanged() throws Exception {
        Path source = tempDir.resolve("DemoSecretProject-source");
        Path target = tempDir.resolve("DemoSecretProject-target");
        SyncRunEntity run = run(source, target);
        run.setRedactionValuesJson(objectMapper.writeValueAsString(List.of(
                new RedactionRuleSnapshot("replacement:old", List.of("DemoSecretProject"), true),
                new RedactionRuleSnapshot("sensitive:old", List.of("DemoTenant"), true)
        )));
        SyncProfileEntity changedProfile = new SyncProfileEntity();
        changedProfile.setName("Changed profile");
        changedProfile.setProfileType(ProfileType.AI_SAFE_EXPORT);
        changedProfile.setSourcePath(source.toString());
        changedProfile.setTargetPath(target.toString());
        run.setProfile(changedProfile);

        SyncRunRepository repository = mock(SyncRunRepository.class);
        when(repository.findById(7L)).thenReturn(Optional.of(run));
        ReportService service = new ReportService(repository, runReportMapper, new ProfileMapper(), new ReportSanitizer(),
                new JsonReportWriter(objectMapper), new CsvReportWriter(), new LeakScanner(), objectMapper);

        String json = service.aiSafeJson(7L);
        String csv = service.aiSafeCsv(7L);

        assertThat(json).doesNotContain("DemoSecretProject", "DemoTenant", source.toString(), target.toString());
        assertThat(csv).doesNotContain("DemoSecretProject", "DemoTenant", source.toString(), target.toString());
        assertThat(json).contains("${SOURCE_ROOT}", "${TARGET_ROOT}", "[REDACTED]");
    }

    private SyncRunEntity run(Path source, Path target) {
        SyncRunEntity run = new SyncRunEntity();
        run.setId(7L);
        run.setProfileName("DemoSecretProject export");
        run.setProfileType(ProfileType.AI_SAFE_EXPORT);
        run.setDryRun(true);
        run.setStartedAt(Instant.parse("2026-01-01T00:00:00Z"));
        run.setEndedAt(Instant.parse("2026-01-01T00:00:01Z"));
        run.setStatus("FAILED");
        run.setConfigHash("hash");
        run.setSourceRoot(source.toString());
        run.setTargetRoot(target.toString());

        SyncReportEntry entry = new SyncReportEntry(
                source.resolve("DemoSecretProject/App.java").toString(),
                target.resolve("DemoTenant/App.java").toString(),
                "DemoSecretProject/App.java",
                "DemoTenant/App.java",
                OperationType.LEAK_DETECTED,
                OperationStatus.BLOCKED,
                Map.of("old", 1),
                Map.of("old", 1),
                Set.of("old"),
                List.of(new com.inneo.aisafecodesync.core.scan.LeakFinding("sensitive:old", "PATH", "DemoTenant/App.java", 1, "DemoTenant remains")),
                "DemoSecretProject was not fully redacted"
        );
        SyncReportEntryEntity entryEntity = runReportMapper.toEntity(entry, run);
        run.getEntries().add(entryEntity);
        return run;
    }
}
