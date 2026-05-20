package com.inneo.aisafecodesync.core.plan;

import com.inneo.aisafecodesync.core.scan.LeakFinding;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncOperation(
        Path sourceAbsolutePath,
        Path targetAbsolutePath,
        String sourceRelativePath,
        String targetRelativePath,
        OperationType operationType,
        OperationStatus operationStatus,
        Map<String, Integer> pathReplacementCounts,
        Map<String, Integer> contentReplacementCounts,
        Set<String> ruleIds,
        List<LeakFinding> leakFindings,
        String errorMessage,
        byte[] plannedBytes
) {

    public SyncOperation {
        pathReplacementCounts = pathReplacementCounts == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(pathReplacementCounts));
        contentReplacementCounts = contentReplacementCounts == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(contentReplacementCounts));
        ruleIds = ruleIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(ruleIds));
        leakFindings = leakFindings == null ? List.of() : List.copyOf(leakFindings);
    }

    public SyncOperation withStatus(OperationStatus status, String errorMessage) {
        return new SyncOperation(
                sourceAbsolutePath,
                targetAbsolutePath,
                sourceRelativePath,
                targetRelativePath,
                operationType,
                status,
                pathReplacementCounts,
                contentReplacementCounts,
                ruleIds,
                leakFindings,
                errorMessage,
                plannedBytes
        );
    }
}
