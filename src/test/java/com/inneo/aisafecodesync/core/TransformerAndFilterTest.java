package com.inneo.aisafecodesync.core;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.filter.PathFilterService;
import com.inneo.aisafecodesync.core.transform.ContentTransformer;
import com.inneo.aisafecodesync.core.transform.PathTransformer;
import com.inneo.aisafecodesync.core.transform.ReplacementEngine;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TransformerAndFilterTest {

    private final ReplacementEngine replacementEngine = new ReplacementEngine();

    @Test
    void pathTransformationAppliesDirectoryAndFileRules() {
        List<ReplacementRule> rules = List.of(
                new ReplacementRule("dir", "CustomerPortal", "demo-app", true, false, true, Set.of(ApplyTarget.DIRECTORY_NAME)),
                new ReplacementRule("file", "Tenant", "Demo", true, false, true, Set.of(ApplyTarget.FILE_NAME))
        );
        PathTransformer transformer = new PathTransformer(replacementEngine);

        PathTransformer.PathTransformResult result = transformer.transformRelativePath(Path.of("CustomerPortal", "TenantService.java"), false, rules);

        assertThat(result.path().toString().replace('\\', '/')).isEqualTo("demo-app/DemoService.java");
        assertThat(result.counts()).containsEntry("dir", 1).containsEntry("file", 1);
    }

    @Test
    void contentTransformationCountsContentRulesOnly() {
        ReplacementRule contentRule = new ReplacementRule("content", "DemoTenant", "demo-tenant", true, false, true, Set.of(ApplyTarget.FILE_CONTENT));
        ReplacementRule pathOnlyRule = new ReplacementRule("path", "DemoTenant", "ignored", true, false, true, Set.of(ApplyTarget.FILE_NAME));
        ContentTransformer transformer = new ContentTransformer(replacementEngine);

        var result = transformer.transform("DemoTenant owns DemoTenant", List.of(contentRule, pathOnlyRule));

        assertThat(result.value()).isEqualTo("demo-tenant owns demo-tenant");
        assertThat(result.counts()).containsOnlyKeys("content");
        assertThat(result.counts()).containsEntry("content", 2);
    }

    @Test
    void includeExcludeFilteringUsesDefaultGlobStyle() {
        PathFilterService filterService = new PathFilterService();
        SyncConfig config = new SyncConfig(
                "filter",
                ProfileType.AI_SAFE_EXPORT,
                Path.of("source"),
                Path.of("target"),
                List.of("**/*.java", "**/*.md"),
                List.of("**/target/**", "**/*secret*"),
                List.of(),
                List.of(),
                false,
                StandardCharsets.UTF_8
        );

        assertThat(filterService.shouldIncludeFile(Path.of("src", "main", "App.java"), config)).isTrue();
        assertThat(filterService.shouldIncludeFile(Path.of("src", "main", "App.class"), config)).isFalse();
        assertThat(filterService.shouldIncludeFile(Path.of("src", "secret-notes.md"), config)).isFalse();
        assertThat(filterService.shouldTraverseDirectory(Path.of("target"), config)).isFalse();
    }

    @Test
    void aiSafeExportKeepsSecretFileExcludesWhenCustomExcludesAreProvided() {
        PathFilterService filterService = new PathFilterService();
        SyncConfig config = new SyncConfig(
                "filter",
                ProfileType.AI_SAFE_EXPORT,
                Path.of("source"),
                Path.of("target"),
                List.of("**/*.properties"),
                List.of("**/generated/**"),
                List.of(),
                List.of(),
                false,
                StandardCharsets.UTF_8
        );

        assertThat(filterService.shouldIncludeFile(Path.of("src", "main", "application-prod.properties"), config)).isFalse();
        assertThat(filterService.shouldIncludeFile(Path.of("src", "main", "safe.properties"), config)).isTrue();
        assertThat(filterService.shouldTraverseDirectory(Path.of("generated"), config)).isFalse();
    }
}
