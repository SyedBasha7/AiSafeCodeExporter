package com.inneo.aisafecodesync.core.report;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CsvReportWriter {

    public String write(SyncReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append("runId,profileName,dryRun,status,sourceRelativePath,targetRelativePath,operationType,operationStatus,pathReplacementCounts,contentReplacementCounts,ruleIds,leakCount,errorMessage\n");
        for (SyncReportEntry entry : report.entries()) {
            csv.append(escape(report.runId())).append(',')
                    .append(escape(report.profileName())).append(',')
                    .append(escape(report.dryRun())).append(',')
                    .append(escape(report.status())).append(',')
                    .append(escape(entry.sourceRelativePath())).append(',')
                    .append(escape(entry.targetRelativePath())).append(',')
                    .append(escape(entry.operationType())).append(',')
                    .append(escape(entry.operationStatus())).append(',')
                    .append(escape(entry.pathReplacementCounts())).append(',')
                    .append(escape(entry.contentReplacementCounts())).append(',')
                    .append(escape(entry.ruleIds().stream().sorted().collect(Collectors.joining("|")))).append(',')
                    .append(escape(entry.leakFindings().size())).append(',')
                    .append(escape(entry.errorMessage()))
                    .append('\n');
        }
        return csv.toString();
    }

    private String escape(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
