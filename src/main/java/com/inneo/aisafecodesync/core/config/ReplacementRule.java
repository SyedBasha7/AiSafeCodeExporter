package com.inneo.aisafecodesync.core.config;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record ReplacementRule(
        String id,
        String search,
        String replacement,
        boolean caseSensitive,
        boolean regex,
        boolean enabled,
        Set<ApplyTarget> applyTo
) {

    public ReplacementRule {
        id = id == null ? "" : id.trim();
        search = search == null ? "" : search;
        replacement = replacement == null ? "" : replacement;
        applyTo = applyTo == null || applyTo.isEmpty()
                ? EnumSet.of(ApplyTarget.DIRECTORY_NAME, ApplyTarget.FILE_NAME, ApplyTarget.FILE_CONTENT)
                : EnumSet.copyOf(applyTo);
    }

    public static ReplacementRule plain(String id, String search, String replacement, ApplyTarget... targets) {
        return new ReplacementRule(id, search, replacement, true, false, true, targets(targets));
    }

    public boolean appliesTo(ApplyTarget target) {
        return enabled && applyTo.contains(Objects.requireNonNull(target));
    }

    private static Set<ApplyTarget> targets(ApplyTarget... targets) {
        if (targets == null || targets.length == 0) {
            return EnumSet.allOf(ApplyTarget.class);
        }
        EnumSet<ApplyTarget> result = EnumSet.noneOf(ApplyTarget.class);
        for (ApplyTarget target : targets) {
            result.add(target);
        }
        return result;
    }
}
