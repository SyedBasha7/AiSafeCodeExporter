package com.inneo.aisafecodesync.core.scan;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class TextFileDetector {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java",
            ".xml",
            ".jsp",
            ".properties",
            ".yml",
            ".yaml",
            ".json",
            ".sql",
            ".md",
            ".txt",
            ".html",
            ".css",
            ".js",
            ".ts",
            ".gradle",
            ".gitignore"
    );

    public boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (".gitignore".equals(fileName)) {
            return true;
        }
        return TEXT_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
