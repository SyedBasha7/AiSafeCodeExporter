package com.inneo.aisafecodesync.core.config;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.StringJoiner;

@Component
public class ConfigHasher {

    public String hash(SyncConfig config) {
        StringJoiner joiner = new StringJoiner("\u001F");
        joiner.add(config.profileName());
        joiner.add(config.profileType().name());
        joiner.add(normalizePath(config.sourceRoot()));
        joiner.add(normalizePath(config.targetRoot()));
        joiner.add(Boolean.toString(config.allowTargetInsideSource()));
        config.includePatterns().forEach(pattern -> joiner.add("I=" + pattern));
        config.excludePatterns().forEach(pattern -> joiner.add("E=" + pattern));
        for (ReplacementRule rule : config.replacementRules()) {
            joiner.add("R=" + rule.id())
                    .add(rule.search())
                    .add(rule.replacement())
                    .add(Boolean.toString(rule.caseSensitive()))
                    .add(Boolean.toString(rule.regex()))
                    .add(Boolean.toString(rule.enabled()))
                    .add(rule.applyTo().stream().map(Enum::name).sorted().toList().toString());
        }
        for (SensitiveTermRule rule : config.sensitiveTermRules()) {
            joiner.add("S=" + rule.id())
                    .add(Boolean.toString(rule.caseSensitive()))
                    .add(Boolean.toString(rule.enabled()));
            rule.values().forEach(joiner::add);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(joiner.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String normalizePath(java.nio.file.Path path) {
        return path == null ? "" : path.toAbsolutePath().normalize().toString();
    }
}
