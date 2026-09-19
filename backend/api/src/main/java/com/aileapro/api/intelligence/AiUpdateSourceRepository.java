package com.aileapro.api.intelligence;

import com.aileapro.persistence.AiUpdateSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AiUpdateSourceRepository extends JpaRepository<AiUpdateSourceEntity, AiUpdateSourceEntity.PK> {

    List<AiUpdateSourceEntity> findByUpdateId(UUID updateId);

    List<AiUpdateSourceEntity> findByUpdateIdIn(Collection<UUID> updateIds);
}
