package com.inneo.aisafecodesync.web.mapper;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SensitiveTermRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.exception.ConfigValidationException;
import com.inneo.aisafecodesync.persistence.entity.ReplacementRuleEntity;
import com.inneo.aisafecodesync.persistence.entity.SensitiveTermRuleEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.dto.ProfileYaml;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProfileMapper {

    public ProfileForm newForm() {
        ProfileForm form = new ProfileForm();
        form.setName("Inneo AI-safe export");
        form.setProfileType(ProfileType.AI_SAFE_EXPORT);
        form.setIncludePatternsText(joinLines(SyncConfig.defaultIncludes()));
        form.setExcludePatternsText(joinLines(SyncConfig.defaultExcludes()));
        form.setReplacementRulesText("# id|search|replacement|caseSensitive|regex|enabled|applyTargets\n"
                + "inneo-project|InneoCustomerPortal|inneo-app|true|false|true|DIRECTORY_NAME,FILE_NAME,FILE_CONTENT");
        form.setSensitiveTermRulesText("# id|caseSensitive|enabled|comma-separated-values\n"
                + "inneo-sensitive|true|true|InneoCustomerPortal,InneoTenant");
        return form;
    }

    public ProfileForm toForm(SyncProfileEntity entity) {
        ProfileForm form = new ProfileForm();
        form.setName(entity.getName());
        form.setProfileType(entity.getProfileType());
        form.setSourcePath(entity.getSourcePath());
        form.setTargetPath(entity.getTargetPath());
        form.setIncludePatternsText(entity.getIncludePatterns());
        form.setExcludePatternsText(entity.getExcludePatterns());
        form.setAllowTargetInsideSource(entity.isAllowTargetInsideSource());
        form.setReplacementRulesText(formatReplacementRules(entity.getReplacementRules()));
        form.setSensitiveTermRulesText(formatSensitiveRules(entity.getSensitiveTermRules()));
        return form;
    }

    public SyncConfig toConfig(SyncProfileEntity entity) {
        return new SyncConfig(
                entity.getName(),
                entity.getProfileType(),
                toPath(entity.getSourcePath()),
                toPath(entity.getTargetPath()),
                splitLines(entity.getIncludePatterns()),
                splitLines(entity.getExcludePatterns()),
                entity.getReplacementRules().stream().map(this::toCore).toList(),
                entity.getSensitiveTermRules().stream().map(this::toCore).toList(),
                entity.isAllowTargetInsideSource(),
                StandardCharsets.UTF_8
        );
    }

    public SyncConfig toConfig(ProfileForm form) {
        return new SyncConfig(
                form.getName(),
                form.getProfileType(),
                toPath(form.getSourcePath()),
                toPath(form.getTargetPath()),
                splitLines(form.getIncludePatternsText()),
                splitLines(form.getExcludePatternsText()),
                parseReplacementRules(form.getReplacementRulesText()),
                parseSensitiveRules(form.getSensitiveTermRulesText()),
                form.isAllowTargetInsideSource(),
                StandardCharsets.UTF_8
        );
    }

    public void updateEntity(SyncProfileEntity entity, ProfileForm form) {
        entity.setName(form.getName().trim());
        entity.setProfileType(form.getProfileType() == null ? ProfileType.AI_SAFE_EXPORT : form.getProfileType());
        entity.setSourcePath(trimToNull(form.getSourcePath()));
        entity.setTargetPath(trimToNull(form.getTargetPath()));
        entity.setIncludePatterns(joinLines(splitLines(form.getIncludePatternsText())));
        entity.setExcludePatterns(joinLines(splitLines(form.getExcludePatternsText())));
        entity.setAllowTargetInsideSource(form.isAllowTargetInsideSource());
        entity.getReplacementRules().clear();
        List<ReplacementRule> replacementRules = parseReplacementRules(form.getReplacementRulesText());
        for (int i = 0; i < replacementRules.size(); i++) {
            ReplacementRuleEntity ruleEntity = toEntity(replacementRules.get(i), i);
            ruleEntity.setProfile(entity);
            entity.getReplacementRules().add(ruleEntity);
        }
        entity.getSensitiveTermRules().clear();
        List<SensitiveTermRule> sensitiveRules = parseSensitiveRules(form.getSensitiveTermRulesText());
        for (int i = 0; i < sensitiveRules.size(); i++) {
            SensitiveTermRuleEntity ruleEntity = toEntity(sensitiveRules.get(i), i);
            ruleEntity.setProfile(entity);
            entity.getSensitiveTermRules().add(ruleEntity);
        }
    }

    public ProfileYaml toYaml(SyncProfileEntity entity) {
        return new ProfileYaml(
                entity.getName(),
                entity.getProfileType(),
                entity.getSourcePath(),
                entity.getTargetPath(),
                splitLines(entity.getIncludePatterns()),
                splitLines(entity.getExcludePatterns()),
                entity.isAllowTargetInsideSource(),
                entity.getReplacementRules().stream()
                        .map(rule -> new ProfileYaml.ReplacementRuleYaml(
                                rule.getRuleId(),
                                rule.getSearchValue(),
                                rule.getReplacementValue(),
                                rule.isCaseSensitive(),
                                rule.isRegex(),
                                rule.isEnabled(),
                                rule.getApplyTargets()))
                        .toList(),
                entity.getSensitiveTermRules().stream()
                        .map(rule -> new ProfileYaml.SensitiveTermRuleYaml(
                                rule.getRuleId(),
                                rule.getValues(),
                                rule.isCaseSensitive(),
                                rule.isEnabled()))
                        .toList()
        );
    }

    public ProfileForm toForm(ProfileYaml yaml) {
        ProfileForm form = new ProfileForm();
        form.setName(yaml.name());
        form.setProfileType(yaml.profileType());
        form.setSourcePath(yaml.sourcePath());
        form.setTargetPath(yaml.targetPath());
        form.setIncludePatternsText(joinLines(yaml.includePatterns()));
        form.setExcludePatternsText(joinLines(yaml.excludePatterns()));
        form.setAllowTargetInsideSource(yaml.allowTargetInsideSource());
        form.setReplacementRulesText(yaml.replacementRules() == null ? "" : yaml.replacementRules().stream()
                .map(rule -> String.join("|",
                        nullSafe(rule.id()),
                        nullSafe(rule.search()),
                        nullSafe(rule.replacement()),
                        Boolean.toString(rule.caseSensitive()),
                        Boolean.toString(rule.regex()),
                        Boolean.toString(rule.enabled()),
                        rule.applyTo() == null ? "" : rule.applyTo().stream().map(Enum::name).collect(Collectors.joining(","))))
                .collect(Collectors.joining(System.lineSeparator())));
        form.setSensitiveTermRulesText(yaml.sensitiveTermRules() == null ? "" : yaml.sensitiveTermRules().stream()
                .map(rule -> String.join("|",
                        nullSafe(rule.id()),
                        Boolean.toString(rule.caseSensitive()),
                        Boolean.toString(rule.enabled()),
                        rule.values() == null ? "" : String.join(",", rule.values())))
                .collect(Collectors.joining(System.lineSeparator())));
        return form;
    }

    public List<ReplacementRule> parseReplacementRules(String text) {
        List<ReplacementRule> rules = new ArrayList<>();
        int lineNumber = 0;
        for (String line : splitRawLines(text)) {
            lineNumber++;
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length < 3) {
                throw new ConfigValidationException(List.of("Replacement rule line " + lineNumber + " must have at least id|search|replacement."));
            }
            rules.add(new ReplacementRule(
                    parts[0].trim(),
                    parts[1],
                    parts[2],
                    parseBoolean(parts, 3, true, lineNumber, "caseSensitive"),
                    parseBoolean(parts, 4, false, lineNumber, "regex"),
                    parseBoolean(parts, 5, true, lineNumber, "enabled"),
                    parseApplyTargets(parts.length > 6 ? parts[6] : "", lineNumber)
            ));
        }
        return rules;
    }

    public List<SensitiveTermRule> parseSensitiveRules(String text) {
        List<SensitiveTermRule> rules = new ArrayList<>();
        int lineNumber = 0;
        for (String line : splitRawLines(text)) {
            lineNumber++;
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                throw new ConfigValidationException(List.of("Sensitive term rule line " + lineNumber + " must have id|caseSensitive|enabled|values."));
            }
            List<String> values = List.of(parts[3].split(",", -1)).stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
            rules.add(new SensitiveTermRule(
                    parts[0].trim(),
                    values,
                    parseBoolean(parts, 1, true, lineNumber, "caseSensitive"),
                    parseBoolean(parts, 2, true, lineNumber, "enabled")
            ));
        }
        return rules;
    }

    private ReplacementRule toCore(ReplacementRuleEntity entity) {
        return new ReplacementRule(entity.getRuleId(), entity.getSearchValue(), entity.getReplacementValue(),
                entity.isCaseSensitive(), entity.isRegex(), entity.isEnabled(), entity.getApplyTargets());
    }

    private SensitiveTermRule toCore(SensitiveTermRuleEntity entity) {
        return new SensitiveTermRule(entity.getRuleId(), entity.getValues(), entity.isCaseSensitive(), entity.isEnabled());
    }

    private ReplacementRuleEntity toEntity(ReplacementRule rule, int sortOrder) {
        ReplacementRuleEntity entity = new ReplacementRuleEntity();
        entity.setSortOrder(sortOrder);
        entity.setRuleId(rule.id());
        entity.setSearchValue(rule.search());
        entity.setReplacementValue(rule.replacement());
        entity.setCaseSensitive(rule.caseSensitive());
        entity.setRegex(rule.regex());
        entity.setEnabled(rule.enabled());
        entity.setApplyTargets(rule.applyTo());
        return entity;
    }

    private SensitiveTermRuleEntity toEntity(SensitiveTermRule rule, int sortOrder) {
        SensitiveTermRuleEntity entity = new SensitiveTermRuleEntity();
        entity.setSortOrder(sortOrder);
        entity.setRuleId(rule.id());
        entity.setValues(new ArrayList<>(rule.values()));
        entity.setCaseSensitive(rule.caseSensitive());
        entity.setEnabled(rule.enabled());
        return entity;
    }

    private String formatReplacementRules(List<ReplacementRuleEntity> rules) {
        return rules.stream()
                .map(rule -> String.join("|",
                        rule.getRuleId(),
                        rule.getSearchValue(),
                        rule.getReplacementValue(),
                        Boolean.toString(rule.isCaseSensitive()),
                        Boolean.toString(rule.isRegex()),
                        Boolean.toString(rule.isEnabled()),
                        rule.getApplyTargets().stream().map(Enum::name).collect(Collectors.joining(","))))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatSensitiveRules(List<SensitiveTermRuleEntity> rules) {
        return rules.stream()
                .map(rule -> String.join("|",
                        rule.getRuleId(),
                        Boolean.toString(rule.isCaseSensitive()),
                        Boolean.toString(rule.isEnabled()),
                        String.join(",", rule.getValues())))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private Set<ApplyTarget> parseApplyTargets(String value, int lineNumber) {
        if (value == null || value.isBlank()) {
            return EnumSet.allOf(ApplyTarget.class);
        }
        EnumSet<ApplyTarget> targets = EnumSet.noneOf(ApplyTarget.class);
        for (String token : value.split(",")) {
            if (!token.isBlank()) {
                try {
                    targets.add(ApplyTarget.valueOf(token.trim()));
                } catch (IllegalArgumentException ex) {
                    throw new ConfigValidationException(List.of("Replacement rule line " + lineNumber + " has an invalid apply target: " + token.trim()));
                }
            }
        }
        return targets.isEmpty() ? EnumSet.allOf(ApplyTarget.class) : targets;
    }

    private boolean parseBoolean(String[] parts, int index, boolean defaultValue, int lineNumber, String fieldName) {
        if (index >= parts.length || parts[index].isBlank()) {
            return defaultValue;
        }
        String value = parts[index].trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new ConfigValidationException(List.of("Rule line " + lineNumber + " has an invalid boolean for " + fieldName + ": " + value));
    }

    private Path toPath(String value) {
        try {
            return value == null || value.isBlank() ? null : Path.of(value.trim());
        } catch (InvalidPathException ex) {
            throw new ConfigValidationException(List.of("Invalid path '" + value + "': " + ex.getReason()));
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> splitLines(String text) {
        return splitRawLines(text).stream()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
    }

    private List<String> splitRawLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return text.lines().toList();
    }

    private String joinLines(List<String> lines) {
        return lines == null ? "" : String.join(System.lineSeparator(), lines);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
