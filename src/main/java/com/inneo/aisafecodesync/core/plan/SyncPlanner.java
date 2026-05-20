package com.inneo.aisafecodesync.core.plan;

import com.inneo.aisafecodesync.core.config.ConfigHasher;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.filter.PathFilterService;
import com.inneo.aisafecodesync.core.scan.BinaryFileDetector;
import com.inneo.aisafecodesync.core.scan.LeakFinding;
import com.inneo.aisafecodesync.core.scan.LeakScanner;
import com.inneo.aisafecodesync.core.scan.TextFileDetector;
import com.inneo.aisafecodesync.core.transform.ContentTransformer;
import com.inneo.aisafecodesync.core.transform.PathTransformer;
import com.inneo.aisafecodesync.core.transform.ReplacementOutcome;
import com.inneo.aisafecodesync.core.validation.SyncConfigValidator;
import com.inneo.aisafecodesync.core.validation.ValidationResult;
import com.inneo.aisafecodesync.exception.ConfigValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SyncPlanner {

    private final SyncConfigValidator validator;
    private final PathFilterService pathFilterService;
    private final PathTransformer pathTransformer;
    private final ContentTransformer contentTransformer;
    private final LeakScanner leakScanner;
    private final TextFileDetector textFileDetector;
    private final BinaryFileDetector binaryFileDetector;
    private final ConfigHasher configHasher;

    public SyncPlanner(
            SyncConfigValidator validator,
            PathFilterService pathFilterService,
            PathTransformer pathTransformer,
            ContentTransformer contentTransformer,
            LeakScanner leakScanner,
            TextFileDetector textFileDetector,
            BinaryFileDetector binaryFileDetector,
            ConfigHasher configHasher
    ) {
        this.validator = validator;
        this.pathFilterService = pathFilterService;
        this.pathTransformer = pathTransformer;
        this.contentTransformer = contentTransformer;
        this.leakScanner = leakScanner;
        this.textFileDetector = textFileDetector;
        this.binaryFileDetector = binaryFileDetector;
        this.configHasher = configHasher;
    }

    public SyncPlan plan(SyncConfig config) {
        String configHash = configHasher.hash(config);
        ValidationResult validation = validator.validate(config);
        List<SyncOperation> operations = new ArrayList<>();
        List<String> blockers = new ArrayList<>(validation.errors());
        if (!validation.valid()) {
            validation.errors().forEach(error -> operations.add(errorOperation(error)));
            return new SyncPlan(config, configHash, operations, false, blockers);
        }

        Path sourceRoot = config.sourceRoot().toAbsolutePath().normalize();
        Path targetRoot = config.targetRoot().toAbsolutePath().normalize();
        Map<String, String> targetFileOwners = new LinkedHashMap<>();
        Set<String> plannedDirectoryTargets = new LinkedHashSet<>();

        try {
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (sourceRoot.equals(dir)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path sourceRelative = sourceRoot.relativize(dir);
                    if (!pathFilterService.shouldTraverseDirectory(sourceRelative, config)) {
                        operations.add(operation(dir, null, sourceRelative, null, OperationType.SKIP_EXCLUDED,
                                OperationStatus.SKIPPED, Map.of(), Map.of(), Set.of(), List.of(), "Excluded directory.", null));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    try {
                        PathTransformer.PathTransformResult transformed = pathTransformer.transformRelativePath(sourceRelative, true, config.replacementRules());
                        Path targetAbsolute = safeTargetPath(targetRoot, transformed.path());
                        String targetKey = normalize(transformed.path());
                        if (Files.exists(targetAbsolute) && !Files.isDirectory(targetAbsolute)) {
                            String message = "Target directory path is an existing file: '" + targetKey + "'.";
                            blockers.add(message);
                            operations.add(operation(dir, targetAbsolute, sourceRelative, transformed.path(), OperationType.CONFLICT,
                                    OperationStatus.BLOCKED, transformed.counts(), Map.of(), transformed.counts().keySet(), List.of(), message, null));
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        if (targetFileOwners.containsKey(targetKey)) {
                            String message = "Source directory maps to a target file path '" + targetKey + "'.";
                            blockers.add(message);
                            operations.add(operation(dir, targetAbsolute, sourceRelative, transformed.path(), OperationType.CONFLICT,
                                    OperationStatus.BLOCKED, transformed.counts(), Map.of(), transformed.counts().keySet(), List.of(), message, null));
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        plannedDirectoryTargets.add(targetKey);
                        if (!sourceRelative.equals(transformed.path())) {
                            operations.add(operation(dir, targetAbsolute, sourceRelative, transformed.path(), OperationType.TRANSFORM_PATH,
                                    OperationStatus.PLANNED, transformed.counts(), Map.of(), transformed.counts().keySet(), List.of(), null, null));
                        }
                        addLeakOperations(config, operations, dir, targetAbsolute, sourceRelative, transformed.path(), leakScanner.scanPath(transformed.path(), config.sensitiveTermRules()));
                        operations.add(operation(dir, targetAbsolute, sourceRelative, transformed.path(),
                                Files.exists(targetAbsolute) ? OperationType.SKIP_UNCHANGED : OperationType.CREATE_DIRECTORY,
                                Files.exists(targetAbsolute) ? OperationStatus.SKIPPED : OperationStatus.PLANNED,
                                transformed.counts(), Map.of(), transformed.counts().keySet(), List.of(), null, null));
                    } catch (ConfigValidationException ex) {
                        blockers.addAll(ex.getErrors());
                        operations.add(operation(dir, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                                Map.of(), Map.of(), Set.of(), List.of(), ex.getMessage(), null));
                    } catch (RuntimeException ex) {
                        blockers.add(ex.getMessage());
                        operations.add(operation(dir, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                                Map.of(), Map.of(), Set.of(), List.of(), ex.getMessage(), null));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path sourceRelative = sourceRoot.relativize(file);
                    if (!pathFilterService.shouldIncludeFile(sourceRelative, config)) {
                        operations.add(operation(file, null, sourceRelative, null, OperationType.SKIP_EXCLUDED,
                                OperationStatus.SKIPPED, Map.of(), Map.of(), Set.of(), List.of(), "Excluded file.", null));
                        return FileVisitResult.CONTINUE;
                    }
                    if (!Files.isReadable(file)) {
                        String message = "Source file is not readable.";
                        blockers.add(message + " " + sourceRelative);
                        operations.add(operation(file, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                                Map.of(), Map.of(), Set.of(), List.of(), message, null));
                        return FileVisitResult.CONTINUE;
                    }
                    planFile(config, sourceRoot, targetRoot, targetFileOwners, plannedDirectoryTargets, operations, blockers, file, sourceRelative);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Path sourceRelative = sourceRoot.relativize(file);
                    String message = "Could not read source path: " + exc.getMessage();
                    blockers.add(message);
                    operations.add(operation(file, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                            Map.of(), Map.of(), Set.of(), List.of(), message, null));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            blockers.add("Could not traverse source folder: " + ex.getMessage());
            operations.add(errorOperation("Could not traverse source folder: " + ex.getMessage()));
        }

        boolean executable = blockers.isEmpty()
                && operations.stream().noneMatch(operation -> operation.operationStatus() == OperationStatus.BLOCKED || operation.operationStatus() == OperationStatus.FAILED);
        return new SyncPlan(config, configHash, operations, executable, blockers);
    }

    private void planFile(
            SyncConfig config,
            Path sourceRoot,
            Path targetRoot,
            Map<String, String> targetFileOwners,
            Set<String> plannedDirectoryTargets,
            List<SyncOperation> operations,
            List<String> blockers,
            Path file,
            Path sourceRelative
    ) {
        try {
            PathTransformer.PathTransformResult pathResult = pathTransformer.transformRelativePath(sourceRelative, false, config.replacementRules());
            Path targetAbsolute = safeTargetPath(targetRoot, pathResult.path());
            if (!sourceRelative.equals(pathResult.path())) {
                operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), OperationType.TRANSFORM_PATH,
                        OperationStatus.PLANNED, pathResult.counts(), Map.of(), pathResult.counts().keySet(), List.of(), null, null));
            }

            String targetKey = normalize(pathResult.path());
            if (plannedDirectoryTargets.contains(targetKey)) {
                String message = "Source file maps to a target directory path '" + targetKey + "'.";
                blockers.add(message);
                operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), OperationType.CONFLICT,
                        OperationStatus.BLOCKED, pathResult.counts(), Map.of(), pathResult.counts().keySet(), List.of(), message, null));
                return;
            }
            String previousOwner = targetFileOwners.putIfAbsent(targetKey, normalize(sourceRelative));
            if (previousOwner != null && !previousOwner.equals(normalize(sourceRelative))) {
                String message = "Multiple source files map to target path '" + targetKey + "'.";
                blockers.add(message);
                operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), OperationType.CONFLICT,
                        OperationStatus.BLOCKED, pathResult.counts(), Map.of(), pathResult.counts().keySet(), List.of(), message, null));
                return;
            }
            if (Files.exists(targetAbsolute) && Files.isDirectory(targetAbsolute)) {
                String message = "Target path is an existing directory.";
                blockers.add(message + " " + targetKey);
                operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), OperationType.CONFLICT,
                        OperationStatus.BLOCKED, pathResult.counts(), Map.of(), pathResult.counts().keySet(), List.of(), message, null));
                return;
            }

            addLeakOperations(config, operations, file, targetAbsolute, sourceRelative, pathResult.path(), leakScanner.scanPath(pathResult.path(), config.sensitiveTermRules()));

            PlannedContent plannedContent = plannedContent(file, config);
            Map<String, Integer> contentCounts = Map.of();
            byte[] plannedBytes = plannedContent.bytes();
            if (plannedContent.text()) {
                ReplacementOutcome contentOutcome = contentTransformer.transform(plannedContent.textContent(), config.replacementRules());
                contentCounts = contentOutcome.counts();
                plannedBytes = contentOutcome.value().getBytes(config.charset());
                if (contentOutcome.totalCount() > 0) {
                    operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), OperationType.TRANSFORM_CONTENT,
                            OperationStatus.PLANNED, Map.of(), contentCounts, contentCounts.keySet(), List.of(), null, null));
                }
                addLeakOperations(config, operations, file, targetAbsolute, sourceRelative, pathResult.path(),
                        leakScanner.scanContent(pathResult.path(), contentOutcome.value(), config.sensitiveTermRules()));
            }

            OperationType operationType = fileOperationType(targetAbsolute, plannedBytes);
            OperationStatus status = operationType == OperationType.SKIP_UNCHANGED ? OperationStatus.SKIPPED : OperationStatus.PLANNED;
            Set<String> ruleIds = new LinkedHashSet<>();
            ruleIds.addAll(pathResult.counts().keySet());
            ruleIds.addAll(contentCounts.keySet());
            operations.add(operation(file, targetAbsolute, sourceRelative, pathResult.path(), operationType, status,
                    pathResult.counts(), contentCounts, ruleIds, List.of(), null, plannedBytes));
        } catch (ConfigValidationException ex) {
            blockers.addAll(ex.getErrors());
            operations.add(operation(file, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                    Map.of(), Map.of(), Set.of(), List.of(), ex.getMessage(), null));
        } catch (CharacterCodingException ex) {
            String message = "Text file could not be decoded with " + config.charset().name() + ".";
            blockers.add(message + " " + sourceRelative);
            operations.add(operation(file, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                    Map.of(), Map.of(), Set.of(), List.of(), message, null));
        } catch (IOException ex) {
            String message = "Could not plan file: " + ex.getMessage();
            blockers.add(message + " " + sourceRelative);
            operations.add(operation(file, null, sourceRelative, null, OperationType.ERROR, OperationStatus.BLOCKED,
                    Map.of(), Map.of(), Set.of(), List.of(), message, null));
        }
    }

    private PlannedContent plannedContent(Path file, SyncConfig config) throws IOException {
        boolean binary = binaryFileDetector.isBinary(file);
        if (binary || !textFileDetector.isTextFile(file)) {
            return new PlannedContent(false, null, Files.readAllBytes(file));
        }
        byte[] bytes = Files.readAllBytes(file);
        String text = config.charset()
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        return new PlannedContent(true, text, bytes);
    }

    private OperationType fileOperationType(Path targetAbsolute, byte[] plannedBytes) throws IOException {
        if (!Files.exists(targetAbsolute)) {
            return OperationType.CREATE_FILE;
        }
        byte[] existing = Files.readAllBytes(targetAbsolute);
        if (java.util.Arrays.equals(existing, plannedBytes)) {
            return OperationType.SKIP_UNCHANGED;
        }
        return OperationType.UPDATE_FILE;
    }

    private Path safeTargetPath(Path targetRoot, Path transformedRelativePath) {
        Path targetAbsolute = targetRoot.resolve(transformedRelativePath).normalize();
        if (!targetAbsolute.startsWith(targetRoot)) {
            throw new ConfigValidationException(List.of("Transformed target path escapes the target folder: " + transformedRelativePath));
        }
        return targetAbsolute;
    }

    private String normalize(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private void addLeakOperations(
            SyncConfig config,
            List<SyncOperation> operations,
            Path sourceAbsolute,
            Path targetAbsolute,
            Path sourceRelative,
            Path targetRelative,
            List<LeakFinding> findings
    ) {
        if (findings.isEmpty()) {
            return;
        }
        OperationStatus status = config.profileType() == ProfileType.AI_SAFE_EXPORT ? OperationStatus.BLOCKED : OperationStatus.PLANNED;
        operations.add(operation(sourceAbsolute, targetAbsolute, sourceRelative, targetRelative, OperationType.LEAK_DETECTED,
                status, Map.of(), Map.of(), findings.stream().map(LeakFinding::ruleId).collect(java.util.stream.Collectors.toSet()),
                findings, "Sensitive term leak detected after transformation.", null));
    }

    private SyncOperation operation(
            Path sourceAbsolute,
            Path targetAbsolute,
            Path sourceRelative,
            Path targetRelative,
            OperationType type,
            OperationStatus status,
            Map<String, Integer> pathCounts,
            Map<String, Integer> contentCounts,
            Set<String> ruleIds,
            List<LeakFinding> findings,
            String errorMessage,
            byte[] plannedBytes
    ) {
        return new SyncOperation(
                sourceAbsolute,
                targetAbsolute,
                sourceRelative == null ? "" : sourceRelative.toString().replace('\\', '/'),
                targetRelative == null ? "" : targetRelative.toString().replace('\\', '/'),
                type,
                status,
                pathCounts,
                contentCounts,
                ruleIds,
                findings,
                errorMessage,
                plannedBytes
        );
    }

    private SyncOperation errorOperation(String message) {
        return new SyncOperation(null, null, "", "", OperationType.ERROR, OperationStatus.BLOCKED,
                Map.of(), Map.of(), Set.of(), List.of(), message, null);
    }

    private record PlannedContent(boolean text, String textContent, byte[] bytes) {
    }
}
