package com.inneo.aisafecodesync.web.dto;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;

import java.util.List;
import java.util.Set;

public record ProfileYaml(
        String name,
        ProfileType profileType,
        String sourcePath,
        String targetPath,
        List<String> includePatterns,
        List<String> excludePatterns,
        boolean allowTargetInsideSource,
        List<ReplacementRuleYaml> replacementRules,
        List<SensitiveTermRuleYaml> sensitiveTermRules
) {

    public record ReplacementRuleYaml(
            String id,
            String search,
            String replacement,
            boolean caseSensitive,
            boolean regex,
            boolean enabled,
            Set<ApplyTarget> applyTo
    ) {
    }

    public record SensitiveTermRuleYaml(
            String id,
            List<String> values,
            boolean caseSensitive,
            boolean enabled
    ) {
    }
}
