package com.aileapro.api.intelligence;

import com.aileapro.persistence.CurriculumChangeProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CurriculumChangeProposalRepository extends JpaRepository<CurriculumChangeProposalEntity, UUID> {

    List<CurriculumChangeProposalEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<CurriculumChangeProposalEntity> findAllByOrderByCreatedAtDesc();

    List<CurriculumChangeProposalEntity> findByUpdateIdOrderByCreatedAtDesc(UUID updateId);
}
