package com.inneo.aisafecodesync.core.report;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record SyncReport(
        Long runId,
        String profileName,
        ProfileType profileType,
        boolean dryRun,
        Instant startedAt,
        Instant endedAt,
        String status,
        String sourceRoot,
        String targetRoot,
        String configHash,
        List<SyncReportEntry> entries
) {

    public SyncReport {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public long durationMillis() {
        if (startedAt == null || endedAt == null) {
            return 0;
        }
        return Duration.between(startedAt, endedAt).toMillis();
    }

    public Map<OperationType, Long> operationCounts() {
        Map<OperationType, Long> counts = new EnumMap<>(OperationType.class);
        entries.forEach(entry -> counts.merge(entry.operationType(), 1L, Long::sum));
        return counts;
    }

    public Map<OperationStatus, Long> statusCounts() {
        Map<OperationStatus, Long> counts = new EnumMap<>(OperationStatus.class);
        entries.forEach(entry -> counts.merge(entry.operationStatus(), 1L, Long::sum));
        return counts;
    }

    public long leakCount() {
        return entries.stream().mapToLong(entry -> entry.leakFindings().size()).sum();
    }
}
