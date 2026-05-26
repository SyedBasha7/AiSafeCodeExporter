package com.inneo.aisafecodesync.web.service;

import com.inneo.aisafecodesync.core.config.ConfigHasher;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.execute.SyncExecutor;
import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.SyncOperation;
import com.inneo.aisafecodesync.core.plan.SyncPlan;
import com.inneo.aisafecodesync.core.plan.SyncPlanner;
import com.inneo.aisafecodesync.core.report.SyncReportEntry;
import com.inneo.aisafecodesync.exception.RunAlreadyActiveException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.inneo.aisafecodesync.persistence.repository.SyncProfileRepository;
import com.inneo.aisafecodesync.persistence.repository.SyncRunRepository;
import com.inneo.aisafecodesync.web.dto.RedactionRuleSnapshot;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.inneo.aisafecodesync.web.mapper.RunReportMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SyncRunCoordinator {

    private final ReentrantLock runLock = new ReentrantLock();
    private final SyncProfileRepository profileRepository;
    private final SyncRunRepository runRepository;
    private final ProfileMapper profileMapper;
    private final RunReportMapper runReportMapper;
    private final SyncPlanner syncPlanner;
    private final SyncExecutor syncExecutor;
    private final ConfigHasher configHasher;
    private final ExecutionEligibilityService executionEligibilityService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SyncRunCoordinator(
            SyncProfileRepository profileRepository,
            SyncRunRepository runRepository,
            ProfileMapper profileMapper,
            RunReportMapper runReportMapper,
            SyncPlanner syncPlanner,
            SyncExecutor syncExecutor,
            ConfigHasher configHasher,
            ExecutionEligibilityService executionEligibilityService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.profileRepository = profileRepository;
        this.runRepository = runRepository;
        this.profileMapper = profileMapper;
        this.runReportMapper = runReportMapper;
        this.syncPlanner = syncPlanner;
        this.syncExecutor = syncExecutor;
        this.configHasher = configHasher;
        this.executionEligibilityService = executionEligibilityService;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.clock = clock;
    }

    @Transactional
    public long runDryRun(long profileId) {
        if (!runLock.tryLock()) {
            throw new RunAlreadyActiveException();
        }
        try {
            SyncProfileEntity profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new NoSuchElementException("Profile not found: " + profileId));
            SyncConfig config = profileMapper.toConfig(profile);
            Instant started = clock.instant();
            SyncPlan plan = syncPlanner.plan(config);
            Instant ended = clock.instant();
            String status = plan.executable() ? "SUCCESS" : "FAILED";
            SyncRunEntity run = persistRun(profile, config, true, started, ended, status, plan.configHash(), plan.operations());
            if (plan.executable() && profile.getProfileType() == ProfileType.AI_SAFE_EXPORT) {
                profile.setLastSuccessfulDryRunHash(plan.configHash());
                profile.setLastSuccessfulDryRunAt(ended);
                profileRepository.save(profile);
            }
            return run.getId();
        } finally {
            runLock.unlock();
        }
    }

    @Transactional
    public long execute(long profileId) {
        if (!runLock.tryLock()) {
            throw new RunAlreadyActiveException();
        }
        try {
            SyncProfileEntity profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new NoSuchElementException("Profile not found: " + profileId));
            SyncConfig config = profileMapper.toConfig(profile);
            String currentHash = configHasher.hash(config);
            executionEligibilityService.requireExecutable(profile, currentHash);
            Instant started = clock.instant();
            SyncPlan plan = syncPlanner.plan(config);
            if (!plan.executable()) {
                SyncRunEntity blockedRun = persistRun(profile, config, false, started, clock.instant(), "BLOCKED", plan.configHash(), plan.operations());
                return blockedRun.getId();
            }
            List<SyncOperation> executedOperations = finalizeActualExecutionStatuses(syncExecutor.execute(plan));
            String status = executedOperations.stream().anyMatch(operation -> operation.operationStatus() == OperationStatus.FAILED)
                    ? "COMPLETED_WITH_ERRORS"
                    : "SUCCESS";
            SyncRunEntity run = persistRun(profile, config, false, started, clock.instant(), status, plan.configHash(), executedOperations);
            return run.getId();
        } finally {
            runLock.unlock();
        }
    }

    private List<SyncOperation> finalizeActualExecutionStatuses(List<SyncOperation> operations) {
        return operations.stream()
                .map(operation -> operation.operationStatus() == OperationStatus.PLANNED
                        ? operation.withStatus(OperationStatus.FAILED, "Execution finished without resolving this planned operation.")
                        : operation)
                .toList();
    }

    private SyncRunEntity persistRun(
            SyncProfileEntity profile,
            SyncConfig config,
            boolean dryRun,
            Instant started,
            Instant ended,
            String status,
            String configHash,
            List<SyncOperation> operations
    ) {
        SyncRunEntity run = new SyncRunEntity();
        run.setProfile(profile);
        run.setProfileName(profile.getName());
        run.setProfileType(profile.getProfileType());
        run.setDryRun(dryRun);
        run.setStartedAt(started);
        run.setEndedAt(ended);
        run.setStatus(status);
        run.setConfigHash(configHash);
        run.setSourceRoot(config.sourceRoot() == null ? "" : config.sourceRoot().toAbsolutePath().normalize().toString());
        run.setTargetRoot(config.targetRoot() == null ? "" : config.targetRoot().toAbsolutePath().normalize().toString());
        run.setRedactionValuesJson(writeRedactionSnapshot(config));
        for (SyncOperation operation : operations) {
            SyncReportEntry entry = runReportMapper.toReportEntry(operation);
            run.getEntries().add(runReportMapper.toEntity(entry, run));
        }
        return runRepository.save(run);
    }

    private String writeRedactionSnapshot(SyncConfig config) {
        List<RedactionRuleSnapshot> snapshots = new ArrayList<>();
        config.replacementRules().stream()
                .filter(rule -> !rule.search().isBlank())
                .forEach(rule -> snapshots.add(new RedactionRuleSnapshot(
                        "replacement:" + rule.id(),
                        List.of(rule.search()),
                        rule.caseSensitive()
                )));
        config.sensitiveTermRules().stream()
                .filter(rule -> !rule.values().isEmpty())
                .forEach(rule -> snapshots.add(new RedactionRuleSnapshot(
                        "sensitive:" + rule.id(),
                        rule.values(),
                        rule.caseSensitive()
                )));
        try {
            return objectMapper.writeValueAsString(snapshots);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize report redaction snapshot.", ex);
        }
    }
}
