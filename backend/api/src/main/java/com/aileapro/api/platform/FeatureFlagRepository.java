package com.aileapro.api.platform;

import com.aileapro.persistence.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {
}
