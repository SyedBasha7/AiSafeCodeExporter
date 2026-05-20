package com.inneo.aisafecodesync.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sensitive_term_rules")
public class SensitiveTermRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private SyncProfileEntity profile;

    private int sortOrder;

    @Column(nullable = false)
    private String ruleId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sensitive_term_values", joinColumns = @JoinColumn(name = "sensitive_term_rule_id"))
    @OrderColumn(name = "value_order")
    @Column(name = "term_value", length = 4000, nullable = false)
    private List<String> values = new ArrayList<>();

    @Column(nullable = false)
    private boolean caseSensitive = true;

    @Column(nullable = false)
    private boolean enabled = true;

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

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
