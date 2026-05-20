package com.inneo.aisafecodesync.core.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

public record SyncConfig(
        String profileName,
        ProfileType profileType,
        Path sourceRoot,
        Path targetRoot,
        List<String> includePatterns,
        List<String> excludePatterns,
        List<ReplacementRule> replacementRules,
        List<SensitiveTermRule> sensitiveTermRules,
        boolean allowTargetInsideSource,
        Charset charset
) {

    public SyncConfig {
        profileName = profileName == null || profileName.isBlank() ? "Unnamed profile" : profileName.trim();
        profileType = profileType == null ? ProfileType.AI_SAFE_EXPORT : profileType;
        includePatterns = includePatterns == null || includePatterns.isEmpty()
                ? defaultIncludes()
                : includePatterns.stream().filter(pattern -> pattern != null && !pattern.isBlank()).map(String::trim).toList();
        List<String> configuredExcludes = excludePatterns == null || excludePatterns.isEmpty()
                ? defaultExcludes()
                : excludePatterns.stream().filter(pattern -> pattern != null && !pattern.isBlank()).map(String::trim).toList();
        if (profileType == ProfileType.AI_SAFE_EXPORT) {
            LinkedHashSet<String> mergedExcludes = new LinkedHashSet<>(defaultExcludes());
            mergedExcludes.addAll(configuredExcludes);
            excludePatterns = List.copyOf(mergedExcludes);
        } else {
            excludePatterns = configuredExcludes;
        }
        replacementRules = replacementRules == null ? List.of() : List.copyOf(replacementRules);
        sensitiveTermRules = sensitiveTermRules == null ? List.of() : List.copyOf(sensitiveTermRules);
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static SyncConfig emptyAiSafeProfile() {
        return new SyncConfig(
                "Inneo AI-safe export",
                ProfileType.AI_SAFE_EXPORT,
                null,
                null,
                defaultIncludes(),
                defaultExcludes(),
                List.of(),
                List.of(),
                false,
                StandardCharsets.UTF_8
        );
    }

    public static List<String> defaultIncludes() {
        return List.of(
                "**/*.java",
                "**/*.xml",
                "**/*.properties",
                "**/*.yml",
                "**/*.yaml",
                "**/*.json",
                "**/*.sql",
                "**/*.md",
                "**/*.txt",
                "**/*.html",
                "**/*.css",
                "**/*.js",
                "**/*.ts",
                "**/*.gradle",
                "**/pom.xml",
                "**/build.gradle",
                "**/settings.gradle"
        );
    }

    public static List<String> defaultExcludes() {
        return List.of(
                "**/target/**",
                "**/build/**",
                "**/out/**",
                "**/.git/**",
                "**/.idea/**",
                "**/.settings/**",
                "**/.vscode/**",
                "**/node_modules/**",
                "**/*.class",
                "**/*.jar",
                "**/*.war",
                "**/*.ear",
                "**/*.zip",
                "**/*.7z",
                "**/*.tar",
                "**/*.gz",
                "**/.env",
                "**/.env.*",
                "**/*secret*",
                "**/*Secret*",
                "**/*credential*",
                "**/*Credential*",
                "**/*credentials*",
                "**/*Credentials*",
                "**/*password*",
                "**/*Password*",
                "**/*passwd*",
                "**/*token*",
                "**/*Token*",
                "**/*.jks",
                "**/*.p12",
                "**/*.pfx",
                "**/*.pem",
                "**/*.key",
                "**/*.crt",
                "**/application-prod.yml",
                "**/application-prod.yaml",
                "**/application-prod.properties",
                "**/application-local.yml",
                "**/application-local.yaml",
                "**/application-local.properties",
                "**/bootstrap-prod.yml",
                "**/bootstrap-prod.yaml",
                "**/bootstrap-prod.properties"
        );
    }
}
