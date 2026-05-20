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
        ReplacementRule rule = new ReplacementRule("customer", "InneoTenant", "inneo-tenant", true, false, true, Set.of(ApplyTarget.FILE_CONTENT));

        ReplacementOutcome outcome = replacementEngine.replace("InneoTenant inneotenant InneoTenant", List.of(rule), ApplyTarget.FILE_CONTENT);

        assertThat(outcome.value()).isEqualTo("inneo-tenant inneotenant inneo-tenant");
        assertThat(outcome.counts()).containsEntry("customer", 2);
    }

    @Test
    void plainTextReplacementCaseInsensitive() {
        ReplacementRule rule = new ReplacementRule("tenant", "InneoTenant", "inneo-tenant", false, false, true, Set.of(ApplyTarget.FILE_CONTENT));

        ReplacementOutcome outcome = replacementEngine.replace("InneoTenant inneotenant INNEOTENANT", List.of(rule), ApplyTarget.FILE_CONTENT);

        assertThat(outcome.value()).isEqualTo("inneo-tenant inneo-tenant inneo-tenant");
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
