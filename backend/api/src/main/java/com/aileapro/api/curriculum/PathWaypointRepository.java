package com.aileapro.api.curriculum;

import com.aileapro.persistence.PathWaypointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PathWaypointRepository extends JpaRepository<PathWaypointEntity, UUID> {

    List<PathWaypointEntity> findByPathIdOrderBySortAsc(UUID pathId);

    void deleteByPathId(UUID pathId);
}
