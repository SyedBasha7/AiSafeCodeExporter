package com.inneo.aisafecodesync.core;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.transform.ReplacementEngine;
import com.inneo.aisafecodesync.core.transform.ReplacementOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReplacementEngineTest {

    private final ReplacementEngine replacementEngine = new ReplacementEngine();

    @Test
    void plainTextReplacementCaseSensitive() {
        ReplacementRule rule = new ReplacementRule("customer", "DemoTenant", "demo-tenant", true, false, true, Set.of(ApplyTarget.FILE_CONTENT));

        ReplacementOutcome outcome = replacementEngine.replace("DemoTenant demotenant DemoTenant", List.of(rule), ApplyTarget.FILE_CONTENT);

        assertThat(outcome.value()).isEqualTo("demo-tenant demotenant demo-tenant");
        assertThat(outcome.counts()).containsEntry("customer", 2);
    }

    @Test
    void plainTextReplacementCaseInsensitive() {
        ReplacementRule rule = new ReplacementRule("tenant", "DemoTenant", "demo-tenant", false, false, true, Set.of(ApplyTarget.FILE_CONTENT));

        ReplacementOutcome outcome = replacementEngine.replace("DemoTenant demotenant DEMOTENANT", List.of(rule), ApplyTarget.FILE_CONTENT);

        assertThat(outcome.value()).isEqualTo("demo-tenant demo-tenant demo-tenant");
        assertThat(outcome.counts()).containsEntry("tenant", 3);
    }

    @Test
    void regexReplacement() {
        ReplacementRule rule = new ReplacementRule("digits", "user-[0-9]+", "user-000", true, true, true, Set.of(ApplyTarget.FILE_CONTENT));

        ReplacementOutcome outcome = replacementEngine.replace("user-123 and user-456", List.of(rule), ApplyTarget.FILE_CONTENT);

        assertThat(outcome.value()).isEqualTo("user-000 and user-000");
        assertThat(outcome.counts()).containsEntry("digits", 2);
    }
}
