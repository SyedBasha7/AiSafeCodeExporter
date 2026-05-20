package com.inneo.aisafecodesync.web.dto;

import java.time.Instant;
import java.util.List;

public record ProfileStatus(
        boolean validationPassed,
        List<String> validationErrors,
        String configHash,
        String lastSuccessfulDryRunHash,
        Instant lastSuccessfulDryRunAt,
        boolean dryRunCurrent,
        boolean executionReady,
        List<String> executionBlocks
) {

    public ProfileStatus {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
        executionBlocks = executionBlocks == null ? List.of() : List.copyOf(executionBlocks);
    }
}
