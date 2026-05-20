package com.inneo.aisafecodesync.core.validation;

import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.filter.PathFilterService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class SyncConfigValidator {

    private final PathFilterService pathFilterService;

    public SyncConfigValidator(PathFilterService pathFilterService) {
        this.pathFilterService = pathFilterService;
    }

    public ValidationResult validate(SyncConfig config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            return new ValidationResult(List.of("Sync configuration is required."));
        }
        if (config.profileName() == null || config.profileName().isBlank()) {
            errors.add("Profile name is required.");
        }
        validateSource(config.sourceRoot(), errors);
        validateTarget(config, errors);
        errors.addAll(pathFilterService.validatePatterns(config.includePatterns()));
        errors.addAll(pathFilterService.validatePatterns(config.excludePatterns()));
        validateReplacementRules(config.replacementRules(), errors);
        validateSensitiveTermRules(config.sensitiveTermRules(), errors);
        return new ValidationResult(errors);
    }

    private void validateSource(Path sourceRoot, List<String> errors) {
        if (sourceRoot == null) {
            errors.add("Source folder is required.");
            return;
        }
        if (!Files.exists(sourceRoot)) {
            errors.add("Source folder does not exist: " + sourceRoot);
            return;
        }
        if (!Files.isDirectory(sourceRoot)) {
            errors.add("Source path must be a folder: " + sourceRoot);
        }
        if (!Files.isReadable(sourceRoot)) {
            errors.add("Source folder is not readable: " + sourceRoot);
        }
    }

    private void validateTarget(SyncConfig config, List<String> errors) {
        Path sourceRoot = config.sourceRoot();
        Path targetRoot = config.targetRoot();
        if (targetRoot == null) {
            errors.add("Target folder is required.");
            return;
        }
        Path absoluteTarget = targetRoot.toAbsolutePath().normalize();
        if (sourceRoot != null) {
            Path absoluteSource = sourceRoot.toAbsolutePath().normalize();
            if (absoluteSource.equals(absoluteTarget)) {
                errors.add("Source and target folders must be different.");
            }
            if (!config.allowTargetInsideSource() && absoluteTarget.startsWith(absoluteSource)) {
                errors.add("Target folder must not be inside the source folder unless the advanced option is enabled.");
            }
        }
        if (Files.exists(targetRoot)) {
            if (!Files.isDirectory(targetRoot)) {
                errors.add("Target path must be a folder when it already exists: " + targetRoot);
            } else if (!Files.isWritable(targetRoot)) {
                errors.add("Target folder is not writable: " + targetRoot);
            }
            return;
        }
        Path existingParent = absoluteTarget.getParent();
        while (existingParent != null && !Files.exists(existingParent)) {
            existingParent = existingParent.getParent();
        }
        if (existingParent == null || !Files.isDirectory(existingParent) || !Files.isWritable(existingParent)) {
            errors.add("Target folder cannot be created because no writable parent folder was found: " + targetRoot);
        }
    }

    private void validateReplacementRules(List<ReplacementRule> rules, List<String> errors) {
        for (ReplacementRule rule : rules) {
            if (rule.id().isBlank()) {
                errors.add("Replacement rule id is required.");
            }
            if (rule.search().isBlank()) {
                errors.add("Replacement rule search value is required for rule '" + rule.id() + "'.");
            }
            if (rule.regex()) {
                try {
                    Pattern.compile(rule.search());
                } catch (PatternSyntaxException ex) {
                    errors.add("Replacement rule '" + rule.id() + "' has an invalid regex: " + ex.getDescription());
                }
            }
        }
    }

    private void validateSensitiveTermRules(List<SensitiveTermRule> rules, List<String> errors) {
        for (SensitiveTermRule rule : rules) {
            if (rule.id().isBlank()) {
                errors.add("Sensitive term rule id is required.");
            }
            if (rule.enabled() && rule.values().isEmpty()) {
                errors.add("Sensitive term rule '" + rule.id() + "' must contain at least one value.");
            }
        }
    }
}
