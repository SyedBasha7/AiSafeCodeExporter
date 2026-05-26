package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.execute.SyncExecutor;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
import com.inneo.aisafecodesync.core.plan.SyncOperation;
import com.inneo.aisafecodesync.core.plan.SyncPlan;
import com.inneo.aisafecodesync.core.plan.SyncPlanner;
import com.inneo.aisafecodesync.exception.RunAlreadyActiveException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.inneo.aisafecodesync.persistence.repository.SyncProfileRepository;
import com.inneo.aisafecodesync.persistence.repository.SyncRunRepository;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.inneo.aisafecodesync.web.mapper.RunReportMapper;
import com.inneo.aisafecodesync.web.service.ExecutionEligibilityService;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SyncRunCoordinatorTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();

    @Test
    void secondRunIsRejectedWhileAnotherRunIsActive() throws Exception {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.createDirectories(target);
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setId(11L);
        profile.setName("Demo profile");
        profile.setProfileType(ProfileType.AI_SAFE_EXPORT);
        profile.setSourcePath(source.toString());
        profile.setTargetPath(target.toString());

        CountDownLatch plannerEntered = new CountDownLatch(1);
        CountDownLatch releasePlanner = new CountDownLatch(1);
        SyncProfileRepository profileRepository = mock(SyncProfileRepository.class);
        SyncRunRepository runRepository = mock(SyncRunRepository.class);
        SyncPlanner planner = mock(SyncPlanner.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(profileRepository.findById(11L)).thenReturn(Optional.of(profile));
        when(runRepository.save(any(SyncRunEntity.class))).thenAnswer(invocation -> {
            SyncRunEntity run = invocation.getArgument(0);
            run.setId(99L);
            return run;
        });
        when(planner.plan(any())).thenAnswer(invocation -> {
            plannerEntered.countDown();
            assertThat(releasePlanner.await(5, TimeUnit.SECONDS)).isTrue();
            var config = invocation.<com.inneo.aisafecodesync.core.config.SyncConfig>getArgument(0);
            return new SyncPlan(config, "hash", List.of(), true, List.of());
        });
        SyncRunCoordinator coordinator = new SyncRunCoordinator(
                profileRepository,
                runRepository,
                new ProfileMapper(),
                new RunReportMapper(objectMapper),
                planner,
                mock(SyncExecutor.class),
                new com.inneo.aisafecodesync.core.config.ConfigHasher(),
                new ExecutionEligibilityService(Clock.fixed(Instant.parse("2026-05-20T10:00:00Z"), ZoneOffset.UTC)),
                objectMapper,
                Clock.fixed(Instant.parse("2026-05-20T10:00:00Z"), ZoneOffset.UTC)
        );

        FutureTask<Long> firstRun = new FutureTask<>(() -> coordinator.runDryRun(11L));
        Thread thread = new Thread(firstRun);
        thread.start();
        assertThat(plannerEntered.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> coordinator.runDryRun(11L))
                .isInstanceOf(RunAlreadyActiveException.class);

        releasePlanner.countDown();
        assertThat(firstRun.get(5, TimeUnit.SECONDS)).isEqualTo(99L);
    }

    @Test
    void actualExecutionDoesNotPersistUnresolvedPlannedEntries() throws Exception {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setId(12L);
        profile.setName("Demo standard sync");
        profile.setProfileType(ProfileType.STANDARD_SYNC);
        profile.setSourcePath(source.toString());
        profile.setTargetPath(target.toString());

        SyncProfileRepository profileRepository = mock(SyncProfileRepository.class);
        SyncRunRepository runRepository = mock(SyncRunRepository.class);
        SyncPlanner planner = mock(SyncPlanner.class);
        SyncExecutor executor = mock(SyncExecutor.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ArgumentCaptor<SyncRunEntity> runCaptor = ArgumentCaptor.forClass(SyncRunEntity.class);

        when(profileRepository.findById(12L)).thenReturn(Optional.of(profile));
        when(runRepository.save(runCaptor.capture())).thenAnswer(invocation -> {
            SyncRunEntity run = invocation.getArgument(0);
            run.setId(100L);
            return run;
        });
        when(planner.plan(any())).thenAnswer(invocation -> {
            var config = invocation.<com.inneo.aisafecodesync.core.config.SyncConfig>getArgument(0);
            return new SyncPlan(config, "hash", List.of(), true, List.of());
        });
        when(executor.execute(any())).thenReturn(List.of(new SyncOperation(
                source.resolve("App.java"),
                target.resolve("App.java"),
                "App.java",
                "App.java",
                OperationType.TRANSFORM_CONTENT,
                OperationStatus.PLANNED,
                Map.of(),
                Map.of(),
                Set.of(),
                List.of(),
                null,
                null
        )));
        SyncRunCoordinator coordinator = new SyncRunCoordinator(
                profileRepository,
                runRepository,
                new ProfileMapper(),
                new RunReportMapper(objectMapper),
                planner,
                executor,
                new com.inneo.aisafecodesync.core.config.ConfigHasher(),
                new ExecutionEligibilityService(Clock.fixed(Instant.parse("2026-05-20T10:00:00Z"), ZoneOffset.UTC)),
                objectMapper,
                Clock.fixed(Instant.parse("2026-05-20T10:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(coordinator.execute(12L)).isEqualTo(100L);

        SyncRunEntity savedRun = runCaptor.getValue();
        assertThat(savedRun.getStatus()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(savedRun.getEntries())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getOperationStatus()).isEqualTo(OperationStatus.FAILED);
                    assertThat(entry.getErrorMessage()).contains("Execution finished without resolving");
                });
    }
}
