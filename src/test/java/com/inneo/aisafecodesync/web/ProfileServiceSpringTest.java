package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.AiSafeCodeSyncApplication;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AiSafeCodeSyncApplication.class)
@ActiveProfiles("test")
class ProfileServiceSpringTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private SyncRunCoordinator syncRunCoordinator;

    @Test
    void importedProfileCanBeMappedToEditFormWithoutOpenSessionInView() throws Exception {
        Path root = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();
        Path source = root.resolve("source");
        Path target = root.resolve("target");
        Files.createDirectories(source);

        long id = profileService.importYaml("""
                name: Imported profile
                profileType: AI_SAFE_EXPORT
                sourcePath: %s
                targetPath: %s
                includePatterns:
                  - "**/*.java"
                excludePatterns:
                  - "**/generated/**"
                allowTargetInsideSource: false
                replacementRules:
                  - id: project-name
                    search: DemoCustomerPortal
                    replacement: demo-app
                    caseSensitive: true
                    regex: false
                    enabled: true
                    applyTo:
                      - DIRECTORY_NAME
                      - FILE_NAME
                      - FILE_CONTENT
                sensitiveTermRules:
                  - id: demo-sensitive
                    values:
                      - DemoCustomerPortal
                    caseSensitive: true
                    enabled: true
                """.formatted(yamlPath(source), yamlPath(target))).getId();

        ProfileForm form = profileService.getForm(id);

        assertThat(form.getName()).isEqualTo("Imported profile");
        assertThat(form.getReplacementRulesText()).contains("project-name|DemoCustomerPortal|demo-app");
        assertThat(form.getSensitiveTermRulesText()).contains("demo-sensitive|true|true|DemoCustomerPortal");
        assertThat(profileService.toConfig(id).replacementRules()).singleElement()
                .satisfies(rule -> assertThat(rule.id()).isEqualTo("project-name"));
    }

    @Test
    void importedProfileCanBeSavedAsDraftWhenPathsDoNotExistYet() {
        long id = profileService.importYaml("""
                name: Draft profile
                profileType: AI_SAFE_EXPORT
                sourcePath: Z:/demo/not-created/source
                targetPath: Z:/demo/not-created/target
                includePatterns:
                  - "**/*.java"
                excludePatterns:
                  - "**/generated/**"
                allowTargetInsideSource: false
                replacementRules:
                  - id: project-name
                    search: DemoCustomerPortal
                    replacement: demo-app
                    caseSensitive: true
                    regex: false
                    enabled: true
                    applyTo:
                      - FILE_CONTENT
                sensitiveTermRules:
                  - id: demo-sensitive
                    values:
                      - DemoCustomerPortal
                    caseSensitive: true
                    enabled: true
                """).getId();

        ProfileForm form = profileService.getForm(id);

        assertThat(form.getName()).isEqualTo("Draft profile");
        assertThat(profileService.validate(id).valid()).isFalse();
        assertThat(profileService.validate(id).errors()).anyMatch(error -> error.contains("Source folder does not exist"));
    }

    @Test
    void savingUnchangedProfileKeepsSuccessfulDryRunCurrent() throws Exception {
        Path root = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();
        Path source = root.resolve("source");
        Path target = root.resolve("target");
        Files.createDirectories(source);
        Files.writeString(source.resolve("App.java"), "class App {}", java.nio.charset.StandardCharsets.UTF_8);

        long id = profileService.importYaml("""
                name: Runnable profile
                profileType: AI_SAFE_EXPORT
                sourcePath: %s
                targetPath: %s
                includePatterns:
                  - "**/*.java"
                excludePatterns:
                  - "**/generated/**"
                allowTargetInsideSource: false
                replacementRules: []
                sensitiveTermRules: []
                """.formatted(yamlPath(source), yamlPath(target))).getId();

        syncRunCoordinator.runDryRun(id);
        assertThat(profileService.status(id).executionReady()).isTrue();

        ProfileForm form = profileService.getForm(id);
        profileService.update(id, form);

        assertThat(profileService.status(id).executionReady()).isTrue();
    }

    private String yamlPath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
