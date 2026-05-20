package com.inneo.aisafecodesync.core.scan;

public record LeakFinding(
        String ruleId,
        String scope,
        String relativePath,
        int occurrenceCount,
        String message
) {
}
