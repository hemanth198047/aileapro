package com.aileapro.api.learning;

import com.aileapro.persistence.LearnerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Keyed by user id, which is also the primary key: one profile per learner. */
public interface LearnerProfileRepository extends JpaRepository<LearnerProfileEntity, UUID> {
}
