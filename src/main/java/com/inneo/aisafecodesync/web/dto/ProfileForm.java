package com.inneo.aisafecodesync.web.dto;

import com.inneo.aisafecodesync.core.config.ProfileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProfileForm {

    @NotBlank
    private String name;

    @NotNull
    private ProfileType profileType = ProfileType.AI_SAFE_EXPORT;

    private String sourcePath;
    private String targetPath;
    private String includePatternsText;
    private String excludePatternsText;
    private boolean allowTargetInsideSource;
    private String replacementRulesText;
    private String sensitiveTermRulesText;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public void setProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getIncludePatternsText() {
        return includePatternsText;
    }

    public void setIncludePatternsText(String includePatternsText) {
        this.includePatternsText = includePatternsText;
    }

    public String getExcludePatternsText() {
        return excludePatternsText;
    }

    public void setExcludePatternsText(String excludePatternsText) {
        this.excludePatternsText = excludePatternsText;
    }

    public boolean isAllowTargetInsideSource() {
        return allowTargetInsideSource;
    }

    public void setAllowTargetInsideSource(boolean allowTargetInsideSource) {
        this.allowTargetInsideSource = allowTargetInsideSource;
    }

    public String getReplacementRulesText() {
        return replacementRulesText;
    }

    public void setReplacementRulesText(String replacementRulesText) {
        this.replacementRulesText = replacementRulesText;
    }

    public String getSensitiveTermRulesText() {
        return sensitiveTermRulesText;
    }

    public void setSensitiveTermRulesText(String sensitiveTermRulesText) {
        this.sensitiveTermRulesText = sensitiveTermRulesText;
    }
}
