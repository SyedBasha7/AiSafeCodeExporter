package com.inneo.aisafecodesync.core.scan;

import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LeakScanner {

    public List<LeakFinding> scanPath(Path transformedRelativePath, List<SensitiveTermRule> rules) {
        String value = transformedRelativePath == null ? "" : transformedRelativePath.toString().replace('\\', '/');
        return scanText(value, rules, "PATH", value);
    }

    public List<LeakFinding> scanContent(Path transformedRelativePath, String transformedContent, List<SensitiveTermRule> rules) {
        String relativePath = transformedRelativePath == null ? "" : transformedRelativePath.toString().replace('\\', '/');
        return scanText(transformedContent == null ? "" : transformedContent, rules, "CONTENT", relativePath);
    }

    public List<LeakFinding> scanReportContent(String reportContent, List<SensitiveTermRule> rules) {
        return scanText(reportContent == null ? "" : reportContent, rules, "REPORT", "report");
    }

    private List<LeakFinding> scanText(String text, List<SensitiveTermRule> rules, String scope, String relativePath) {
        List<LeakFinding> findings = new ArrayList<>();
        if (rules == null || rules.isEmpty() || text.isEmpty()) {
            return findings;
        }
        for (SensitiveTermRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }
            int total = 0;
            for (String term : rule.values()) {
                total += countOccurrences(text, term, rule.caseSensitive());
            }
            if (total > 0) {
                findings.add(new LeakFinding(
                        rule.id(),
                        scope,
                        relativePath,
                        total,
                        "Sensitive term rule '" + rule.id() + "' matched in " + scope.toLowerCase(Locale.ROOT) + "."
                ));
            }
        }
        return findings;
    }

    private int countOccurrences(String text, String term, boolean caseSensitive) {
        if (term == null || term.isBlank()) {
            return 0;
        }
        String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String needle = caseSensitive ? term : term.toLowerCase(Locale.ROOT);
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
