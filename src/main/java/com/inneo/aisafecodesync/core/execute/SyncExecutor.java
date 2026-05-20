package com.inneo.aisafecodesync.core.execute;

import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.plan.SyncOperation;
import com.inneo.aisafecodesync.core.plan.SyncPlan;
import com.inneo.aisafecodesync.exception.PlanNotExecutableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class SyncExecutor {

    public List<SyncOperation> execute(SyncPlan plan) {
        if (!plan.executable()) {
            throw new PlanNotExecutableException("The sync plan contains blocking planning errors and cannot be executed.");
        }
        List<SyncOperation> executed = new ArrayList<>();
        for (SyncOperation operation : plan.operations()) {
            try {
                executed.add(executeOperation(operation, plan.config().targetRoot().toAbsolutePath().normalize()));
            } catch (IOException | RuntimeException ex) {
                executed.add(operation.withStatus(OperationStatus.FAILED, ex.getMessage()));
            }
        }
        return executed;
    }

    private SyncOperation executeOperation(SyncOperation operation, Path targetRoot) throws IOException {
        if (operation.operationStatus() == OperationStatus.SKIPPED
                || operation.operationType() == OperationType.TRANSFORM_PATH
                || operation.operationType() == OperationType.TRANSFORM_CONTENT) {
            return operation.withStatus(operation.operationStatus(), operation.errorMessage());
        }
        if (operation.operationType() == OperationType.CREATE_DIRECTORY) {
            requireInsideTargetRoot(operation.targetAbsolutePath(), targetRoot);
            Files.createDirectories(operation.targetAbsolutePath());
            return operation.withStatus(OperationStatus.SUCCESS, null);
        }
        if (operation.operationType() == OperationType.CREATE_FILE || operation.operationType() == OperationType.UPDATE_FILE) {
            requireInsideTargetRoot(operation.targetAbsolutePath(), targetRoot);
            if (operation.plannedBytes() == null) {
                throw new IOException("Planned file bytes are missing for " + operation.targetRelativePath());
            }
            Path parent = operation.targetAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(operation.targetAbsolutePath(), operation.plannedBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return operation.withStatus(OperationStatus.SUCCESS, null);
        }
        return operation;
    }

    private void requireInsideTargetRoot(Path targetPath, Path targetRoot) throws IOException {
        if (targetPath == null) {
            throw new IOException("Planned operation is missing a target path.");
        }
        Path normalizedTarget = targetPath.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(targetRoot)) {
            throw new IOException("Refusing to write outside target root: " + normalizedTarget);
        }
    }
}
