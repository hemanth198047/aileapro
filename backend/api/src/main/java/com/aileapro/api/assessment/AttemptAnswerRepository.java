package com.aileapro.api.assessment;

import com.aileapro.persistence.AttemptAnswerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswerEntity, UUID> {

    List<AttemptAnswerEntity> findByAttemptIdOrderBySort(UUID attemptId);

    Optional<AttemptAnswerEntity> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    int countByAttemptId(UUID attemptId);

    /**
     * What this learner has recently got wrong on one unit, newest first.
     *
     * <p>Stems rather than ids, because the only consumer is the tutor's context block and what
     * it needs is the question as the learner saw it. {@code correct = false} is deliberate over
     * {@code not correct = true}: an unmarked answer is null, and null is not a mistake.
     */
    @Query("select q.stem from AttemptAnswerEntity a, AttemptEntity t, QuestionEntity q "
            + "where a.attemptId = t.id and a.questionId = q.id "
            + "and t.userId = :userId and q.unitId = :unitId and a.correct = false "
            + "order by a.answeredAt desc")
    List<String> recentWrongStems(
            @Param("userId") UUID userId, @Param("unitId") UUID unitId, Pageable limit);
}
