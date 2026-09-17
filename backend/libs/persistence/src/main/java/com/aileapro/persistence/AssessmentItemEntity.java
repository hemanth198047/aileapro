package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Which items an authored assessment contains, in order, and what each is worth. */
@Entity
@Table(name = "assessment_items")
public class AssessmentItemEntity {

    @Embeddable
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "assessment_id", nullable = false)
        private UUID assessmentId;

        @Column(name = "question_id", nullable = false)
        private UUID questionId;

        public Key() {
        }

        public Key(UUID assessmentId, UUID questionId) {
            this.assessmentId = assessmentId;
            this.questionId = questionId;
        }

        public UUID getAssessmentId() { return assessmentId; }
        public void setAssessmentId(UUID assessmentId) { this.assessmentId = assessmentId; }
        public UUID getQuestionId() { return questionId; }
        public void setQuestionId(UUID questionId) { this.questionId = questionId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(assessmentId, key.assessmentId)
                    && Objects.equals(questionId, key.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(assessmentId, questionId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int sort;

    @Column(nullable = false)
    private BigDecimal points = BigDecimal.ONE;

    public AssessmentItemEntity() {
    }

    public AssessmentItemEntity(UUID assessmentId, UUID questionId, int sort, BigDecimal points) {
        this.id = new Key(assessmentId, questionId);
        this.sort = sort;
        this.points = points;
    }

    public Key getId() { return id; }
    public void setId(Key id) { this.id = id; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
}
