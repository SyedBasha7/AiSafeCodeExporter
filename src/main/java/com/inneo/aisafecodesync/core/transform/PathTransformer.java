package com.inneo.aisafecodesync.core.transform;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.exception.ConfigValidationException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PathTransformer {

    private final ReplacementEngine replacementEngine;

    public PathTransformer(ReplacementEngine replacementEngine) {
        this.replacementEngine = replacementEngine;
    }

    public PathTransformResult transformRelativePath(Path relativePath, boolean directory, List<ReplacementRule> rules) {
        if (relativePath == null || relativePath.getNameCount() == 0) {
            return new PathTransformResult(Path.of(""), Map.of());
        }
        Map<String, Integer> totalCounts = new LinkedHashMap<>();
        Path transformed = null;
        for (int i = 0; i < relativePath.getNameCount(); i++) {
            String originalSegment = relativePath.getName(i).toString();
            ApplyTarget target = i == relativePath.getNameCount() - 1 && !directory
                    ? ApplyTarget.FILE_NAME
                    : ApplyTarget.DIRECTORY_NAME;
            ReplacementOutcome outcome = replacementEngine.replace(originalSegment, rules, target);
            validateSegment(outcome.value(), originalSegment);
            transformed = transformed == null ? Path.of(outcome.value()) : transformed.resolve(outcome.value());
            outcome.counts().forEach((id, count) -> totalCounts.merge(id, count, Integer::sum));
        }
        return new PathTransformResult(transformed, totalCounts);
    }

    private void validateSegment(String transformedSegment, String originalSegment) {
        if (transformedSegment == null || transformedSegment.isBlank()) {
            throw new ConfigValidationException(List.of("Path replacement produced a blank segment for '" + originalSegment + "'."));
        }
        if (".".equals(transformedSegment) || "..".equals(transformedSegment)) {
            throw new ConfigValidationException(List.of("Path replacement produced an unsafe segment for '" + originalSegment + "'."));
        }
        if (transformedSegment.contains("/") || transformedSegment.contains("\\")) {
            throw new ConfigValidationException(List.of("Path replacement for '" + originalSegment + "' must not introduce path separators."));
        }
        Path.of(transformedSegment);
    }

    public record PathTransformResult(Path path, Map<String, Integer> counts) {
    }
}
