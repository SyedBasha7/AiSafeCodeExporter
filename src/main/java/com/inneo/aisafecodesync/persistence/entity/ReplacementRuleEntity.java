package com.inneo.aisafecodesync.persistence.entity;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "replacement_rules")
public class ReplacementRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private SyncProfileEntity profile;

    private int sortOrder;

    @Column(nullable = false)
    private String ruleId;

    @Column(length = 4000, nullable = false)
    private String searchValue;

    @Column(length = 4000, nullable = false)
    private String replacementValue;

    @Column(nullable = false)
    private boolean caseSensitive = true;

    @Column(nullable = false)
    private boolean regex;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "replacement_rule_apply_targets", joinColumns = @JoinColumn(name = "replacement_rule_id"))
    @Column(name = "apply_target", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ApplyTarget> applyTargets = EnumSet.allOf(ApplyTarget.class);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SyncProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(SyncProfileEntity profile) {
        this.profile = profile;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public String getReplacementValue() {
        return replacementValue;
    }

    public void setReplacementValue(String replacementValue) {
        this.replacementValue = replacementValue;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isRegex() {
        return regex;
    }

    public void setRegex(boolean regex) {
        this.regex = regex;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<ApplyTarget> getApplyTargets() {
        return applyTargets;
    }

    public void setApplyTargets(Set<ApplyTarget> applyTargets) {
        this.applyTargets = applyTargets;
    }
}
