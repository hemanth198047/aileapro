package com.aileapro.api.learning;

import com.aileapro.persistence.LearnerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/** Keyed by user id, which is also the primary key: one profile per learner. */
public interface LearnerProfileRepository extends JpaRepository<LearnerProfileEntity, UUID> {

    @Query("select count(p) from LearnerProfileEntity p where p.onboardedAt is not null")
    long countOnboarded();
}
