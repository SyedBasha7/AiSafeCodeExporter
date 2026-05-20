package com.inneo.aisafecodesync.persistence.entity;

import com.inneo.aisafecodesync.core.plan.OperationStatus;
import com.inneo.aisafecodesync.core.plan.OperationType;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_report_entries")
public class SyncReportEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private SyncRunEntity run;

    @Column(length = 2000)
    private String sourcePath;

    @Column(length = 2000)
    private String targetPath;

    @Column(length = 2000)
    private String sourceRelativePath;

    @Column(length = 2000)
    private String targetRelativePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus operationStatus;

    @Lob
    private String pathReplacementCountsJson;

    @Lob
    private String contentReplacementCountsJson;

    @Column(length = 2000)
    private String ruleIdsCsv;

    @Lob
    private String leakFindingsJson;

    @Lob
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SyncRunEntity getRun() {
        return run;
    }

    public void setRun(SyncRunEntity run) {
        this.run = run;
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

    public String getSourceRelativePath() {
        return sourceRelativePath;
    }

    public void setSourceRelativePath(String sourceRelativePath) {
        this.sourceRelativePath = sourceRelativePath;
    }

    public String getTargetRelativePath() {
        return targetRelativePath;
    }

    public void setTargetRelativePath(String targetRelativePath) {
        this.targetRelativePath = targetRelativePath;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getPathReplacementCountsJson() {
        return pathReplacementCountsJson;
    }

    public void setPathReplacementCountsJson(String pathReplacementCountsJson) {
        this.pathReplacementCountsJson = pathReplacementCountsJson;
    }

    public String getContentReplacementCountsJson() {
        return contentReplacementCountsJson;
    }

    public void setContentReplacementCountsJson(String contentReplacementCountsJson) {
        this.contentReplacementCountsJson = contentReplacementCountsJson;
    }

    public String getRuleIdsCsv() {
        return ruleIdsCsv;
    }

    public void setRuleIdsCsv(String ruleIdsCsv) {
        this.ruleIdsCsv = ruleIdsCsv;
    }

    public String getLeakFindingsJson() {
        return leakFindingsJson;
    }

    public void setLeakFindingsJson(String leakFindingsJson) {
        this.leakFindingsJson = leakFindingsJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
