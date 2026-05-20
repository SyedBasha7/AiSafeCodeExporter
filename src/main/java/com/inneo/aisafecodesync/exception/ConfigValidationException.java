package com.inneo.aisafecodesync.exception;

import java.util.List;

public class ConfigValidationException extends AiSafeCodeSyncException {

    private final List<String> errors;

    public ConfigValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
