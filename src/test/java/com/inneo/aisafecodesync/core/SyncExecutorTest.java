package com.inneo.aisafecodesync.core;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.execute.SyncExecutor;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.plan.SyncOperation;
import com.inneo.aisafecodesync.core.plan.SyncPlan;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SyncExecutorTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();

    @Test
    void actualExecutionFinalizesInformationalOperations() throws Exception {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);

        List<SyncOperation> executed = new SyncExecutor().execute(plan(source, target, List.of(
                operation(OperationType.TRANSFORM_PATH, OperationStatus.PLANNED, source.resolve("Demo.java"),
                        target.resolve("safe-demo.java"), "Demo.java", "safe-demo.java", null),
                operation(OperationType.TRANSFORM_CONTENT, OperationStatus.PLANNED, source.resolve("Demo.java"),
                        target.resolve("safe-demo.java"), "Demo.java", "safe-demo.java", null),
                operation(OperationType.LEAK_DETECTED, OperationStatus.PLANNED, source.resolve("Demo.java"),
                        target.resolve("safe-demo.java"), "Demo.java", "safe-demo.java", null)
        )));

        assertThat(executed).noneMatch(operation -> operation.operationStatus() == OperationStatus.PLANNED);
        assertThat(executed)
                .extracting(SyncOperation::operationStatus)
                .containsOnly(OperationStatus.SUCCESS);
    }

    @Test
    void fileOperationsAreExecutedAndFinalized() throws Exception {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Path targetFile = target.resolve("src/App.java");

        List<SyncOperation> executed = new SyncExecutor().execute(plan(source, target, List.of(
                operation(OperationType.CREATE_FILE, OperationStatus.PLANNED, source.resolve("App.java"),
                        targetFile, "App.java", "src/App.java", "class App {}".getBytes(StandardCharsets.UTF_8))
        )));

        assertThat(Files.readString(targetFile, StandardCharsets.UTF_8)).isEqualTo("class App {}");
        assertThat(executed)
                .singleElement()
                .extracting(SyncOperation::operationStatus)
                .isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    void unsupportedPlannedOperationFailsInsteadOfRemainingPlanned() throws Exception {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);

        List<SyncOperation> executed = new SyncExecutor().execute(plan(source, target, List.of(
                operation(OperationType.ERROR, OperationStatus.PLANNED, source.resolve("App.java"),
                        target.resolve("App.java"), "App.java", "App.java", null)
        )));

        assertThat(executed)
                .singleElement()
                .satisfies(operation -> {
                    assertThat(operation.operationStatus()).isEqualTo(OperationStatus.FAILED);
                    assertThat(operation.errorMessage()).contains("No execution action is defined");
                });
    }

    private SyncPlan plan(Path source, Path target, List<SyncOperation> operations) {
        SyncConfig config = new SyncConfig(
                "test",
                ProfileType.STANDARD_SYNC,
                source,
                target,
                List.of("**/*"),
                List.of(),
                List.of(),
                List.of(),
                false,
                StandardCharsets.UTF_8
        );
        return new SyncPlan(config, "hash", operations, true, List.of());
    }

    private SyncOperation operation(
            OperationType type,
            OperationStatus status,
            Path sourceAbsolutePath,
            Path targetAbsolutePath,
            String sourceRelativePath,
            String targetRelativePath,
            byte[] plannedBytes
    ) {
        return new SyncOperation(
                sourceAbsolutePath,
                targetAbsolutePath,
                sourceRelativePath,
                targetRelativePath,
                type,
                status,
                Map.of(),
                Map.of(),
                Set.of(),
                List.of(),
                null,
                plannedBytes
        );
    }
}
