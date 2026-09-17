package com.aileapro.api.assessment;

import com.aileapro.persistence.AttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswerEntity, UUID> {

    List<AttemptAnswerEntity> findByAttemptIdOrderBySort(UUID attemptId);

    Optional<AttemptAnswerEntity> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    int countByAttemptId(UUID attemptId);
}
