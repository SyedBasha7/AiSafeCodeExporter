package com.inneo.aisafecodesync.web.mapper;

import com.inneo.aisafecodesync.core.plan.SyncOperation;
import com.inneo.aisafecodesync.core.report.SyncReport;
import com.inneo.aisafecodesync.core.report.SyncReportEntry;
import com.inneo.aisafecodesync.core.scan.LeakFinding;
import com.inneo.aisafecodesync.persistence.entity.SyncReportEntryEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RunReportMapper {

    private static final TypeReference<Map<String, Integer>> COUNT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<LeakFinding>> LEAK_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public RunReportMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    public SyncReportEntry toReportEntry(SyncOperation operation) {
        return new SyncReportEntry(
                pathToString(operation.sourceAbsolutePath()),
                pathToString(operation.targetAbsolutePath()),
                operation.sourceRelativePath(),
                operation.targetRelativePath(),
                operation.operationType(),
                operation.operationStatus(),
                operation.pathReplacementCounts(),
                operation.contentReplacementCounts(),
                operation.ruleIds(),
                operation.leakFindings(),
                operation.errorMessage()
        );
    }

    public SyncReport toReport(SyncRunEntity entity) {
        return new SyncReport(
                entity.getId(),
                entity.getProfileName(),
                entity.getProfileType(),
                entity.isDryRun(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getStatus(),
                entity.getSourceRoot(),
                entity.getTargetRoot(),
                entity.getConfigHash(),
                entity.getEntries().stream().map(this::toReportEntry).toList()
        );
    }

    public SyncReportEntryEntity toEntity(SyncReportEntry entry, SyncRunEntity run) {
        SyncReportEntryEntity entity = new SyncReportEntryEntity();
        entity.setRun(run);
        entity.setSourcePath(entry.sourcePath());
        entity.setTargetPath(entry.targetPath());
        entity.setSourceRelativePath(entry.sourceRelativePath());
        entity.setTargetRelativePath(entry.targetRelativePath());
        entity.setOperationType(entry.operationType());
        entity.setOperationStatus(entry.operationStatus());
        entity.setPathReplacementCountsJson(writeJson(entry.pathReplacementCounts()));
        entity.setContentReplacementCountsJson(writeJson(entry.contentReplacementCounts()));
        entity.setRuleIdsCsv(entry.ruleIds().stream().sorted().collect(Collectors.joining(",")));
        entity.setLeakFindingsJson(writeJson(entry.leakFindings()));
        entity.setErrorMessage(entry.errorMessage());
        return entity;
    }

    private SyncReportEntry toReportEntry(SyncReportEntryEntity entity) {
        return new SyncReportEntry(
                entity.getSourcePath(),
                entity.getTargetPath(),
                entity.getSourceRelativePath(),
                entity.getTargetRelativePath(),
                entity.getOperationType(),
                entity.getOperationStatus(),
                readMap(entity.getPathReplacementCountsJson()),
                readMap(entity.getContentReplacementCountsJson()),
                readRuleIds(entity.getRuleIdsCsv()),
                readLeaks(entity.getLeakFindingsJson()),
                entity.getErrorMessage()
        );
    }

    private String pathToString(Path path) {
        return path == null ? "" : path.toAbsolutePath().normalize().toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize report metadata.", ex);
        }
    }

    private Map<String, Integer> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, COUNT_MAP);
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<LeakFinding> readLeaks(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LEAK_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Set<String> readRuleIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
