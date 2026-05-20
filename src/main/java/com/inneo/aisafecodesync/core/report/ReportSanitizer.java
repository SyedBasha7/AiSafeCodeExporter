package com.inneo.aisafecodesync.core.report;

import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.scan.LeakFinding;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ReportSanitizer {

    public SyncReport sanitizeForAiExport(SyncReport report, SyncConfig config) {
        Set<String> unsafeRelativePaths = unsafeRelativePaths(report);
        List<SyncReportEntry> entries = report.entries().stream()
                .map(entry -> sanitizeEntry(entry, config, unsafeRelativePaths))
                .toList();
        return new SyncReport(
                report.runId(),
                redactText(report.profileName(), config),
                report.profileType(),
                report.dryRun(),
                report.startedAt(),
                report.endedAt(),
                report.status(),
                "${SOURCE_ROOT}",
                "${TARGET_ROOT}",
                report.configHash(),
                entries
        );
    }

    public String redactText(String value, SyncConfig config) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = value;
        if (config.sourceRoot() != null) {
            redacted = replaceIgnoreSeparators(redacted, config.sourceRoot().toAbsolutePath().normalize().toString(), "${SOURCE_ROOT}");
        }
        if (config.targetRoot() != null) {
            redacted = replaceIgnoreSeparators(redacted, config.targetRoot().toAbsolutePath().normalize().toString(), "${TARGET_ROOT}");
        }
        for (ReplacementRule rule : config.replacementRules()) {
            redacted = redactLiteral(redacted, rule.search(), rule.caseSensitive());
        }
        for (SensitiveTermRule rule : config.sensitiveTermRules()) {
            for (String sensitiveValue : rule.values()) {
                redacted = redactLiteral(redacted, sensitiveValue, rule.caseSensitive());
            }
        }
        return redacted;
    }

    private SyncReportEntry sanitizeEntry(SyncReportEntry entry, SyncConfig config, Set<String> unsafeRelativePaths) {
        List<LeakFinding> findings = entry.leakFindings().stream()
                .map(finding -> new LeakFinding(
                        finding.ruleId(),
                        finding.scope(),
                        "PATH".equals(finding.scope()) ? "[REDACTED_PATH_WITH_LEAK]" : redactText(finding.relativePath(), config),
                        finding.occurrenceCount(),
                        redactText(finding.message(), config)
                ))
                .toList();
        boolean unsafePath = unsafeRelativePaths.contains(normalize(entry.sourceRelativePath()))
                || unsafeRelativePaths.contains(normalize(entry.targetRelativePath()));
        return new SyncReportEntry(
                sanitizeAbsolute(entry.sourcePath(), config, true),
                sanitizeAbsolute(entry.targetPath(), config, false),
                unsafePath ? "[REDACTED_PATH_WITH_LEAK]" : redactText(entry.sourceRelativePath(), config),
                unsafePath ? "[REDACTED_PATH_WITH_LEAK]" : redactText(entry.targetRelativePath(), config),
                entry.operationType(),
                entry.operationStatus(),
                entry.pathReplacementCounts(),
                entry.contentReplacementCounts(),
                entry.ruleIds(),
                findings,
                redactText(entry.errorMessage(), config)
        );
    }

    private Set<String> unsafeRelativePaths(SyncReport report) {
        Set<String> unsafePaths = new HashSet<>();
        for (SyncReportEntry entry : report.entries()) {
            entry.leakFindings().stream()
                    .filter(finding -> "PATH".equals(finding.scope()))
                    .map(finding -> normalize(finding.relativePath()))
                    .forEach(unsafePaths::add);
        }
        return unsafePaths;
    }

    private String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    private String sanitizeAbsolute(String value, SyncConfig config, boolean source) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if ((source && config.sourceRoot() == null) || (!source && config.targetRoot() == null)) {
            return redactText(value, config);
        }
        String root = source
                ? config.sourceRoot().toAbsolutePath().normalize().toString()
                : config.targetRoot().toAbsolutePath().normalize().toString();
        String placeholder = source ? "${SOURCE_ROOT}" : "${TARGET_ROOT}";
        return redactText(replaceIgnoreSeparators(value, root, placeholder), config);
    }

    private String replaceIgnoreSeparators(String value, String search, String replacement) {
        if (search == null || search.isBlank()) {
            return value;
        }
        String result = value.replace(search, replacement);
        String alternate = search.contains("\\") ? search.replace('\\', '/') : search.replace('/', '\\');
        return result.replace(alternate, replacement);
    }

    private String redactLiteral(String value, String literal, boolean caseSensitive) {
        if (value == null || literal == null || literal.isBlank()) {
            return value;
        }
        if (caseSensitive) {
            return value.replace(literal, "[REDACTED]");
        }
        return java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(literal),
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(value)
                .replaceAll("[REDACTED]");
    }
}
