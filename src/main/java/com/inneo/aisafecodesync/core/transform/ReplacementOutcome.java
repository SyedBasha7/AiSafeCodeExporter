package com.inneo.aisafecodesync.core.transform;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReplacementOutcome(String value, Map<String, Integer> counts) {

    public ReplacementOutcome {
        value = value == null ? "" : value;
        counts = counts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    public boolean changedFrom(String original) {
        return !value.equals(original);
    }

    public int totalCount() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
