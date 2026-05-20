package com.inneo.aisafecodesync.persistence.entity;

import com.inneo.aisafecodesync.core.config.ProfileType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sync_profiles")
public class SyncProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileType profileType = ProfileType.AI_SAFE_EXPORT;

    @Column(length = 2000)
    private String sourcePath;

    @Column(length = 2000)
    private String targetPath;

    @Column(length = 8000)
    private String includePatterns;

    @Column(length = 8000)
    private String excludePatterns;

    @Column(nullable = false)
    private boolean allowTargetInsideSource;

    private String lastSuccessfulDryRunHash;

    private Instant lastSuccessfulDryRunAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReplacementRuleEntity> replacementRules = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SensitiveTermRuleEntity> sensitiveTermRules = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(String includePatterns) {
        this.includePatterns = includePatterns;
    }

    public String getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(String excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public boolean isAllowTargetInsideSource() {
        return allowTargetInsideSource;
    }

    public void setAllowTargetInsideSource(boolean allowTargetInsideSource) {
        this.allowTargetInsideSource = allowTargetInsideSource;
    }

    public String getLastSuccessfulDryRunHash() {
        return lastSuccessfulDryRunHash;
    }

    public void setLastSuccessfulDryRunHash(String lastSuccessfulDryRunHash) {
        this.lastSuccessfulDryRunHash = lastSuccessfulDryRunHash;
    }

    public Instant getLastSuccessfulDryRunAt() {
        return lastSuccessfulDryRunAt;
    }

    public void setLastSuccessfulDryRunAt(Instant lastSuccessfulDryRunAt) {
        this.lastSuccessfulDryRunAt = lastSuccessfulDryRunAt;
    }

    public List<ReplacementRuleEntity> getReplacementRules() {
        return replacementRules;
    }

    public void setReplacementRules(List<ReplacementRuleEntity> replacementRules) {
        this.replacementRules = replacementRules;
    }

    public List<SensitiveTermRuleEntity> getSensitiveTermRules() {
        return sensitiveTermRules;
    }

    public void setSensitiveTermRules(List<SensitiveTermRuleEntity> sensitiveTermRules) {
        this.sensitiveTermRules = sensitiveTermRules;
    }
}
