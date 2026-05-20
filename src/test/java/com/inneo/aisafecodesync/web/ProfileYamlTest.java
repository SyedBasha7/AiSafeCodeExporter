package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.persistence.entity.ReplacementRuleEntity;
import com.inneo.aisafecodesync.persistence.entity.SensitiveTermRuleEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.dto.ProfileYaml;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileYamlTest {

    private final Path tempDir = Path.of("target", "test-work", getClass().getSimpleName(), UUID.randomUUID().toString()).toAbsolutePath();

    private final ProfileMapper profileMapper = new ProfileMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @Test
    void yamlProfileImportExportRoundTrip() throws Exception {
        SyncProfileEntity entity = profileEntity();

        String yaml = yamlMapper.writeValueAsString(profileMapper.toYaml(entity));
        ProfileYaml imported = yamlMapper.readValue(yaml, ProfileYaml.class);
        ProfileForm form = profileMapper.toForm(imported);

        assertThat(yaml).contains("InneoTenant");
        assertThat(yaml).doesNotContain("syncRuns");
        assertThat(form.getName()).isEqualTo("Fake export");
        assertThat(profileMapper.toConfig(form).replacementRules()).singleElement()
                .satisfies(rule -> {
                    assertThat(rule.id()).isEqualTo("tenant");
                    assertThat(rule.applyTo()).contains(ApplyTarget.DIRECTORY_NAME, ApplyTarget.FILE_CONTENT);
                });
    }

    private SyncProfileEntity profileEntity() {
        SyncProfileEntity profile = new SyncProfileEntity();
        profile.setName("Fake export");
        profile.setProfileType(ProfileType.AI_SAFE_EXPORT);
        profile.setSourcePath(tempDir.resolve("source").toString());
        profile.setTargetPath(tempDir.resolve("target").toString());
        profile.setIncludePatterns("**/*.java");
        profile.setExcludePatterns("**/target/**");

        ReplacementRuleEntity replacement = new ReplacementRuleEntity();
        replacement.setProfile(profile);
        replacement.setSortOrder(0);
        replacement.setRuleId("tenant");
        replacement.setSearchValue("InneoTenant");
        replacement.setReplacementValue("inneo-tenant");
        replacement.setCaseSensitive(true);
        replacement.setRegex(false);
        replacement.setEnabled(true);
        replacement.setApplyTargets(EnumSet.of(ApplyTarget.DIRECTORY_NAME, ApplyTarget.FILE_CONTENT));
        profile.getReplacementRules().add(replacement);

        SensitiveTermRuleEntity sensitive = new SensitiveTermRuleEntity();
        sensitive.setProfile(profile);
        sensitive.setSortOrder(0);
        sensitive.setRuleId("tenant-sensitive");
        sensitive.setValues(List.of("InneoTenant"));
        sensitive.setCaseSensitive(true);
        sensitive.setEnabled(true);
        profile.getSensitiveTermRules().add(sensitive);
        return profile;
    }
}
