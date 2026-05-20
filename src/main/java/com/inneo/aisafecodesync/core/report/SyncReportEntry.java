package com.inneo.aisafecodesync.core.report;

import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.scan.LeakFinding;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncReportEntry(
        String sourcePath,
        String targetPath,
        String sourceRelativePath,
        String targetRelativePath,
        OperationType operationType,
        OperationStatus operationStatus,
        Map<String, Integer> pathReplacementCounts,
        Map<String, Integer> contentReplacementCounts,
        Set<String> ruleIds,
        List<LeakFinding> leakFindings,
        String errorMessage
) {
}
