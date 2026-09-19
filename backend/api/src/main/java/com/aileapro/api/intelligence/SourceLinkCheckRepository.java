package com.aileapro.api.intelligence;

import com.aileapro.persistence.SourceLinkCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SourceLinkCheckRepository extends JpaRepository<SourceLinkCheckEntity, UUID> {
}
