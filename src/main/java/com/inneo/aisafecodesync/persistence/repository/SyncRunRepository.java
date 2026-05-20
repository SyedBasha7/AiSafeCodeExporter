package com.inneo.aisafecodesync.persistence.repository;

import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncRunRepository extends JpaRepository<SyncRunEntity, Long> {

    List<SyncRunEntity> findTop50ByOrderByStartedAtDesc();

    List<SyncRunEntity> findByProfileId(Long profileId);
}
