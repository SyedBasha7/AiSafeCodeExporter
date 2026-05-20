package com.inneo.aisafecodesync.core;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.report.CsvReportWriter;
import com.inneo.aisafecodesync.core.report.JsonReportWriter;
import com.inneo.aisafecodesync.core.report.ReportSanitizer;
import com.inneo.aisafecodesync.core.report.SyncReport;
import com.inneo.aisafecodesync.core.report.SyncReportEntry;
import com.inneo.aisafecodesync.core.scan.LeakFinding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWriterTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();

    @Test
    void aiSafeReportRedactsAbsolutePathsReplacementSearchValuesAndSensitiveTerms() {
        Path source = tempDir.resolve("private-source");
        Path target = tempDir.resolve("private-target");
        SyncConfig config = new SyncConfig(
                "InneoTenant export",
                ProfileType.AI_SAFE_EXPORT,
                source,
                target,
                List.of("**/*.java"),
                List.of(),
                List.of(new ReplacementRule("project", "InneoSecretProject", "inneo-project", true, false, true, Set.of(ApplyTarget.FILE_CONTENT))),
                List.of(new SensitiveTermRule("tenant", List.of("InneoTenant"), true, true)),
                false,
                StandardCharsets.UTF_8
        );
        SyncReport report = report(source, target);

        SyncReport sanitized = new ReportSanitizer().sanitizeForAiExport(report, config);
        String json = new JsonReportWriter(new ObjectMapper()).write(sanitized);

        assertThat(json).doesNotContain(source.toAbsolutePath().normalize().toString());
        assertThat(json).doesNotContain(target.toAbsolutePath().normalize().toString());
        assertThat(json).doesNotContain("InneoSecretProject");
        assertThat(json).doesNotContain("InneoTenant");
        assertThat(json).contains("${SOURCE_ROOT}");
        assertThat(json).contains("${TARGET_ROOT}");
        assertThat(json).contains("[REDACTED]");
    }

    @Test
    void jsonReportGenerationProducesStructuredReport() {
        SyncReport report = report(tempDir.resolve("source"), tempDir.resolve("target"));

        String json = new JsonReportWriter(new ObjectMapper()).write(report);

        assertThat(json).contains("\"runId\" : 42");
        assertThat(json).contains("\"operationType\" : \"LEAK_DETECTED\"");
    }

    @Test
    void csvReportGenerationProducesRows() {
        SyncReport report = report(tempDir.resolve("source"), tempDir.resolve("target"));

        String csv = new CsvReportWriter().write(report);

        assertThat(csv).startsWith("runId,profileName,dryRun,status");
        assertThat(csv).contains("\"42\"");
        assertThat(csv).contains("\"LEAK_DETECTED\"");
    }

    private SyncReport report(Path source, Path target) {
        SyncReportEntry entry = new SyncReportEntry(
                source.resolve("src/App.java").toString(),
                target.resolve("src/App.java").toString(),
                "src/App.java",
                "InneoTenant/App.java",
                OperationType.LEAK_DETECTED,
                OperationStatus.BLOCKED,
                Map.of("project", 1),
                Map.of("project", 2),
                Set.of("project", "tenant"),
                List.of(new LeakFinding("tenant", "PATH", "InneoTenant/App.java", 1, "Sensitive InneoTenant remains")),
                "Do not expose InneoSecretProject"
        );
        return new SyncReport(42L, "InneoTenant export", ProfileType.AI_SAFE_EXPORT, true,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z"),
                "FAILED", source.toString(), target.toString(), "abc", List.of(entry));
    }
}
