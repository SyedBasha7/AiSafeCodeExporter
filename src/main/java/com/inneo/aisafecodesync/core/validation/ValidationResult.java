package com.inneo.aisafecodesync.core.validation;

import java.util.List;

public record ValidationResult(List<String> errors) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
