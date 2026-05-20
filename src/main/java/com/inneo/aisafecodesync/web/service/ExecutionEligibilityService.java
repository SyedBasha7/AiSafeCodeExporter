package com.inneo.aisafecodesync.web.service;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.exception.PlanNotExecutableException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutionEligibilityService {

    public static final Duration RECENT_DRY_RUN_WINDOW = Duration.ofMinutes(30);

    private final Clock clock;

    public ExecutionEligibilityService(Clock clock) {
        this.clock = clock;
    }

    public List<String> explainExecutionBlocks(SyncProfileEntity profile, String currentConfigHash) {
        List<String> blocks = new ArrayList<>();
        if (profile.getProfileType() != ProfileType.AI_SAFE_EXPORT) {
            return blocks;
        }
        if (profile.getLastSuccessfulDryRunHash() == null || profile.getLastSuccessfulDryRunAt() == null) {
            blocks.add("AI-safe export requires a successful dry-run before actual execution.");
            return blocks;
        }
        if (!profile.getLastSuccessfulDryRunHash().equals(currentConfigHash)) {
            blocks.add("The profile changed after the last successful dry-run. Run dry-run again before execution.");
        }
        Instant threshold = clock.instant().minus(RECENT_DRY_RUN_WINDOW);
        if (profile.getLastSuccessfulDryRunAt().isBefore(threshold)) {
            blocks.add("The last successful dry-run is older than " + RECENT_DRY_RUN_WINDOW.toMinutes() + " minutes.");
        }
        return blocks;
    }

    public void requireExecutable(SyncProfileEntity profile, String currentConfigHash) {
        List<String> blocks = explainExecutionBlocks(profile, currentConfigHash);
        if (!blocks.isEmpty()) {
            throw new PlanNotExecutableException(String.join(" ", blocks));
        }
    }
}
