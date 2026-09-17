package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One item served in one attempt, and what came back.
 *
 * <p>The row is created when the item is served and filled in when it is answered. Serving
 * and answering are different facts: an adaptive attempt needs to know what it has already
 * asked, and a report needs to distinguish a skipped item from one never shown.
 */
@Entity
@Table(name = "attempt_answers")
public class AttemptAnswerEntity {

    @Id
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int sort;

    @Column(nullable = false)
    private BigDecimal points = BigDecimal.ONE;

    /** Exactly what was submitted, so a re-scored item can be re-marked from the original. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String response;

    /** Null means unmarked, which is not the same as wrong. */
    private Boolean correct;

    private BigDecimal score;

    /** Frozen as shown: if the explanation is rewritten, the report still tells the truth. */
    @Column(columnDefinition = "text")
    private String feedback;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "misconception_codes", nullable = false)
    private String[] misconceptionCodes = new String[0];

    @Column(name = "served_at", nullable = false)
    private Instant servedAt = Instant.now();

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "elapsed_seconds")
    private Integer elapsedSeconds;

    @Column(nullable = false)
    private boolean unverified;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String[] getMisconceptionCodes() { return misconceptionCodes; }
    public void setMisconceptionCodes(String[] misconceptionCodes) { this.misconceptionCodes = misconceptionCodes; }
    public Instant getServedAt() { return servedAt; }
    public void setServedAt(Instant servedAt) { this.servedAt = servedAt; }
    public Instant getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(Instant answeredAt) { this.answeredAt = answeredAt; }
    public Integer getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(Integer elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public boolean isUnverified() { return unverified; }
    public void setUnverified(boolean unverified) { this.unverified = unverified; }
    public boolean isAnswered() { return answeredAt != null; }
}
