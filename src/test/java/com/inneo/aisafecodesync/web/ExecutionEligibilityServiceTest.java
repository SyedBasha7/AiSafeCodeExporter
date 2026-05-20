package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.exception.PlanNotExecutableException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.web.service.ExecutionEligibilityService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionEligibilityServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-20T10:00:00Z"), ZoneOffset.UTC);
    private final ExecutionEligibilityService service = new ExecutionEligibilityService(clock);

    @Test
    void dryRunRequiredBeforeExecutionForAiSafeExport() {
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setProfileType(ProfileType.AI_SAFE_EXPORT);

        assertThatThrownBy(() -> service.requireExecutable(profile, "hash"))
                .isInstanceOf(PlanNotExecutableException.class)
                .hasMessageContaining("successful dry-run");
    }

    @Test
    void configHashChangesBlockExecution() {
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setProfileType(ProfileType.AI_SAFE_EXPORT);
        profile.setLastSuccessfulDryRunHash("old");
        profile.setLastSuccessfulDryRunAt(clock.instant());

        assertThat(service.explainExecutionBlocks(profile, "new"))
                .anyMatch(block -> block.contains("profile changed"));
    }

    @Test
    void standardSyncDoesNotRequireDryRun() {
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setProfileType(ProfileType.STANDARD_SYNC);

        assertThat(service.explainExecutionBlocks(profile, "hash")).isEmpty();
    }
}
