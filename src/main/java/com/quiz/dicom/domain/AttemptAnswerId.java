package com.quiz.dicom.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AttemptAnswerId implements Serializable {

    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "option_id")
    private Long optionId;

    public AttemptAnswerId() {}

    public AttemptAnswerId(Long attemptId, Long questionId, Long optionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.optionId = optionId;
    }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttemptAnswerId that)) return false;
        return Objects.equals(attemptId, that.attemptId)
                && Objects.equals(questionId, that.questionId)
                && Objects.equals(optionId, that.optionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attemptId, questionId, optionId);
    }
}
