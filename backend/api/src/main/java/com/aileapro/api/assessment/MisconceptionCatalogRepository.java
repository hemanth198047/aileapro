package com.aileapro.api.assessment;

import com.aileapro.persistence.MisconceptionCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Keyed by code, because the code is what an item cites and what a flag records. */
public interface MisconceptionCatalogRepository extends JpaRepository<MisconceptionCatalogEntity, String> {

    List<MisconceptionCatalogEntity> findByCodeIn(Collection<String> codes);

    List<MisconceptionCatalogEntity> findByUnitIdIn(Collection<UUID> unitIds);
}
