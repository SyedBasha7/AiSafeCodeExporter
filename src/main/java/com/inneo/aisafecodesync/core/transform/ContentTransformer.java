package com.inneo.aisafecodesync.core.transform;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentTransformer {

    private final ReplacementEngine replacementEngine;

    public ContentTransformer(ReplacementEngine replacementEngine) {
        this.replacementEngine = replacementEngine;
    }

    public ReplacementOutcome transform(String content, List<ReplacementRule> rules) {
        return replacementEngine.replace(content, rules, ApplyTarget.FILE_CONTENT);
    }
}
