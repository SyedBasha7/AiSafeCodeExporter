package com.inneo.aisafecodesync.persistence.repository;

import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncProfileRepository extends JpaRepository<SyncProfileEntity, Long> {
}
