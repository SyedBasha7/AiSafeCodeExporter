package com.inneo.aisafecodesync.core.plan;

import com.inneo.aisafecodesync.core.config.SyncConfig;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record SyncPlan(
        SyncConfig config,
        String configHash,
        List<SyncOperation> operations,
        boolean executable,
        List<String> blockingErrors
) {

    public SyncPlan {
        operations = operations == null ? List.of() : List.copyOf(operations);
        blockingErrors = blockingErrors == null ? List.of() : List.copyOf(blockingErrors);
    }

    public Map<OperationType, Long> operationCounts() {
        Map<OperationType, Long> counts = new EnumMap<>(OperationType.class);
        for (SyncOperation operation : operations) {
            counts.merge(operation.operationType(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<OperationStatus, Long> statusCounts() {
        Map<OperationStatus, Long> counts = new EnumMap<>(OperationStatus.class);
        for (SyncOperation operation : operations) {
            counts.merge(operation.operationStatus(), 1L, Long::sum);
        }
        return counts;
    }
}
