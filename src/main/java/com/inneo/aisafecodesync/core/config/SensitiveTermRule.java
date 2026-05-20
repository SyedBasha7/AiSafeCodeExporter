package com.inneo.aisafecodesync.core.config;

import java.util.List;

public record SensitiveTermRule(
        String id,
        List<String> values,
        boolean caseSensitive,
        boolean enabled
) {

    public SensitiveTermRule {
        id = id == null ? "" : id.trim();
        values = values == null
                ? List.of()
                : values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
    }
}
