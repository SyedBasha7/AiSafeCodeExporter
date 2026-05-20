package com.inneo.aisafecodesync.core.filter;

import com.inneo.aisafecodesync.core.config.SyncConfig;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class PathFilterService {

    public boolean shouldTraverseDirectory(Path relativeDirectory, SyncConfig config) {
        if (relativeDirectory == null || relativeDirectory.getNameCount() == 0) {
            return true;
        }
        return !matchesAny(config.excludePatterns(), relativeDirectory, true);
    }

    public boolean shouldIncludeFile(Path relativeFile, SyncConfig config) {
        return !matchesAny(config.excludePatterns(), relativeFile, false)
                && (config.includePatterns().isEmpty() || matchesAny(config.includePatterns(), relativeFile, false));
    }

    public boolean isExcluded(Path relativePath, boolean directory, SyncConfig config) {
        return matchesAny(config.excludePatterns(), relativePath, directory);
    }

    public List<String> validatePatterns(List<String> patterns) {
        List<String> errors = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                Pattern.compile(toRegex(pattern));
            } catch (PatternSyntaxException ex) {
                errors.add("Invalid glob pattern '" + pattern + "': " + ex.getDescription());
            }
        }
        return errors;
    }

    private boolean matchesAny(List<String> patterns, Path path, boolean directory) {
        for (String pattern : patterns) {
            if (matches(pattern, path, directory)) {
                return true;
            }
        }
        return false;
    }

    boolean matches(String pattern, Path path, boolean directory) {
        String candidate = normalize(path);
        if (directory && !candidate.endsWith("/")) {
            candidate = candidate + "/";
        }
        return Pattern.compile(toRegex(pattern)).matcher(candidate).matches();
    }

    private String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String toRegex(String glob) {
        String pattern = glob == null ? "" : glob.trim().replace('\\', '/');
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                boolean doubleStar = i + 1 < pattern.length() && pattern.charAt(i + 1) == '*';
                if (doubleStar) {
                    boolean followedBySlash = i + 2 < pattern.length() && pattern.charAt(i + 2) == '/';
                    if (followedBySlash) {
                        regex.append("(?:.*/)?");
                        i += 2;
                    } else {
                        regex.append(".*");
                        i++;
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (c == '?') {
                regex.append("[^/]");
            } else {
                if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }
}
