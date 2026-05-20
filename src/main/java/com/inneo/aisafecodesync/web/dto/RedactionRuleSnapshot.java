package com.inneo.aisafecodesync.web.dto;

import java.util.List;

public record RedactionRuleSnapshot(String id, List<String> values, boolean caseSensitive) {

    public RedactionRuleSnapshot {
        id = id == null || id.isBlank() ? "redaction" : id.trim();
        values = values == null
                ? List.of()
                : values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList();
    }
}
