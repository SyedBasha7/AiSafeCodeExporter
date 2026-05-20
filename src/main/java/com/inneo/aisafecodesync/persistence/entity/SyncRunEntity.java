package com.inneo.aisafecodesync.persistence.entity;

import com.inneo.aisafecodesync.core.config.ProfileType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sync_runs")
public class SyncRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private SyncProfileEntity profile;

    @Column(nullable = false)
    private String profileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileType profileType;

    @Column(nullable = false)
    private boolean dryRun;

    private Instant startedAt;

    private Instant endedAt;

    @Column(nullable = false)
    private String status;

    private String configHash;

    @Column(length = 2000)
    private String sourceRoot;

    @Column(length = 2000)
    private String targetRoot;

    @Lob
    private String redactionValuesJson;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SyncReportEntryEntity> entries = new ArrayList<>();

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

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public void setProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigHash() {
        return configHash;
    }

    public void setConfigHash(String configHash) {
        this.configHash = configHash;
    }

    public String getSourceRoot() {
        return sourceRoot;
    }

    public void setSourceRoot(String sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    public String getTargetRoot() {
        return targetRoot;
    }

    public void setTargetRoot(String targetRoot) {
        this.targetRoot = targetRoot;
    }

    public String getRedactionValuesJson() {
        return redactionValuesJson;
    }

    public void setRedactionValuesJson(String redactionValuesJson) {
        this.redactionValuesJson = redactionValuesJson;
    }

    public List<SyncReportEntryEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<SyncReportEntryEntity> entries) {
        this.entries = entries;
    }
}
