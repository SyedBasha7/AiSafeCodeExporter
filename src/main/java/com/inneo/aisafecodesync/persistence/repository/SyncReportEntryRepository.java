package com.inneo.aisafecodesync.persistence.repository;

import com.inneo.aisafecodesync.persistence.entity.SyncReportEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncReportEntryRepository extends JpaRepository<SyncReportEntryEntity, Long> {
}
