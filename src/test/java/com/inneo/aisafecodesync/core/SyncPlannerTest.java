package com.inneo.aisafecodesync.core;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ConfigHasher;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.filter.PathFilterService;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.plan.SyncPlan;
import com.inneo.aisafecodesync.core.plan.SyncPlanner;
import com.inneo.aisafecodesync.core.scan.BinaryFileDetector;
import com.inneo.aisafecodesync.core.scan.LeakScanner;
import com.inneo.aisafecodesync.core.scan.TextFileDetector;
import com.inneo.aisafecodesync.core.transform.ContentTransformer;
import com.inneo.aisafecodesync.core.transform.PathTransformer;
import com.inneo.aisafecodesync.core.transform.ReplacementEngine;
import com.inneo.aisafecodesync.core.validation.SyncConfigValidator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SyncPlannerTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();

    @Test
    void binaryFileIsNotContentTransformed() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        byte[] binary = new byte[]{0, 'E', 'x', 'a', 'm', 'p', 'l', 'e'};
        Files.write(source.resolve("data.bin"), binary);
        SyncConfig config = config(source, target,
                List.of("**/*.bin"),
                List.of(),
                List.of(new ReplacementRule("content", "Inneo", "Safe", true, false, true, Set.of(ApplyTarget.FILE_CONTENT))),
                List.of());

        SyncPlan plan = planner().plan(config);

        assertThat(plan.operations()).noneMatch(operation -> operation.operationType() == OperationType.TRANSFORM_CONTENT);
        assertThat(plan.operations())
                .filteredOn(operation -> operation.operationType() == OperationType.CREATE_FILE)
                .singleElement()
                .satisfies(operation -> assertThat(operation.plannedBytes()).isEqualTo(binary));
    }

    @Test
    void detectsConflictForTwoSourceFilesMappingToSameTarget() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source.resolve("CustomerA"));
        Files.createDirectories(source.resolve("CustomerB"));
        Files.writeString(source.resolve("CustomerA").resolve("Service.java"), "class A {}", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("CustomerB").resolve("Service.java"), "class B {}", StandardCharsets.UTF_8);
        List<ReplacementRule> rules = List.of(
                new ReplacementRule("a", "CustomerA", "Inneo", true, false, true, Set.of(ApplyTarget.DIRECTORY_NAME)),
                new ReplacementRule("b", "CustomerB", "Inneo", true, false, true, Set.of(ApplyTarget.DIRECTORY_NAME))
        );

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), rules, List.of()));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.operations()).anyMatch(operation -> operation.operationType() == OperationType.CONFLICT
                && operation.operationStatus() == OperationStatus.BLOCKED);
    }

    @Test
    void detectsFileMappingToPlannedDirectoryBeforeExecution() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source.resolve("InneoDirectory"));
        Files.writeString(source.resolve("InneoFile.java"), "class InneoFile {}", StandardCharsets.UTF_8);
        List<ReplacementRule> rules = List.of(
                new ReplacementRule("file-to-dir", "InneoFile.java", "InneoDirectory", true, false, true, Set.of(ApplyTarget.FILE_NAME))
        );

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), rules, List.of()));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.operations()).anyMatch(operation -> operation.operationType() == OperationType.CONFLICT
                && operation.operationStatus() == OperationStatus.BLOCKED
                && operation.errorMessage().contains("target directory path"));
    }

    @Test
    void detectsSensitiveLeakInTransformedContent() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.writeString(source.resolve("App.java"), "class App { String tenant = \"InneoTenant\"; }", StandardCharsets.UTF_8);

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), List.of(),
                List.of(new SensitiveTermRule("tenant", List.of("InneoTenant"), true, true))));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.operations()).anyMatch(operation -> operation.operationType() == OperationType.LEAK_DETECTED
                && operation.operationStatus() == OperationStatus.BLOCKED
                && operation.leakFindings().stream().anyMatch(finding -> finding.scope().equals("CONTENT")));
    }

    @Test
    void detectsSensitiveLeakInTransformedPath() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.writeString(source.resolve("InneoTenant.java"), "class InneoTenant {}", StandardCharsets.UTF_8);

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), List.of(),
                List.of(new SensitiveTermRule("tenant", List.of("InneoTenant"), true, true))));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.operations()).anyMatch(operation -> operation.operationType() == OperationType.LEAK_DETECTED
                && operation.leakFindings().stream().anyMatch(finding -> finding.scope().equals("PATH")));
    }

    @Test
    void sourceEqualsTargetValidationBlocksPlanning() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Files.createDirectories(source);

        SyncPlan plan = planner().plan(config(source, source, List.of("**/*.java"), List.of(), List.of(), List.of()));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.blockingErrors()).anyMatch(error -> error.contains("Source and target folders must be different"));
    }

    @Test
    void targetInsideSourceValidationBlocksPlanningUnlessAllowed() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Files.createDirectories(source);
        Path target = source.resolve("export");

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), List.of(), List.of()));

        assertThat(plan.executable()).isFalse();
        assertThat(plan.blockingErrors()).anyMatch(error -> error.contains("Target folder must not be inside"));
    }

    @Test
    void skipUnchangedFiles() throws Exception {
        Files.createDirectories(tempDir);
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.createDirectories(target);
        Files.writeString(source.resolve("App.java"), "class App {}", StandardCharsets.UTF_8);
        Files.writeString(target.resolve("App.java"), "class App {}", StandardCharsets.UTF_8);

        SyncPlan plan = planner().plan(config(source, target, List.of("**/*.java"), List.of(), List.of(), List.of()));

        assertThat(plan.executable()).isTrue();
        assertThat(plan.operations()).anyMatch(operation -> operation.operationType() == OperationType.SKIP_UNCHANGED
                && operation.operationStatus() == OperationStatus.SKIPPED
                && operation.targetRelativePath().equals("App.java"));
    }

    private SyncPlanner planner() {
        PathFilterService filterService = new PathFilterService();
        ReplacementEngine replacementEngine = new ReplacementEngine();
        TextFileDetector textFileDetector = new TextFileDetector();
        return new SyncPlanner(
                new SyncConfigValidator(filterService),
                filterService,
                new PathTransformer(replacementEngine),
                new ContentTransformer(replacementEngine),
                new LeakScanner(),
                textFileDetector,
                new BinaryFileDetector(textFileDetector),
                new ConfigHasher()
        );
    }

    private SyncConfig config(
            Path source,
            Path target,
            List<String> includes,
            List<String> excludes,
            List<ReplacementRule> replacementRules,
            List<SensitiveTermRule> sensitiveTermRules
    ) {
        return new SyncConfig("test", ProfileType.AI_SAFE_EXPORT, source, target, includes, excludes,
                replacementRules, sensitiveTermRules, false, StandardCharsets.UTF_8);
    }
}
