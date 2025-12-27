package com.quiz.dicom.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "attempt_answer",
        indexes = {
                @Index(name = "ix_attempt_answer_attempt_id", columnList = "attempt_id"),
                @Index(name = "ix_attempt_answer_question_id", columnList = "question_id")
        }
)
public class AttemptAnswer {

    @EmbeddedId
    private AttemptAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("optionId")
    @JoinColumn(name = "option_id", nullable = false)
    private QuestionOption option;

    @Column(name = "selected_at", nullable = false)
    private OffsetDateTime selectedAt = OffsetDateTime.now();

    public AttemptAnswer() {}

    public AttemptAnswer(Attempt attempt, Question question, QuestionOption option) {
        this.attempt = attempt;
        this.question = question;
        this.option = option;
        this.id = new AttemptAnswerId(attempt.getId(), question.getId(), option.getId());
    }

    // getters/setters

    public AttemptAnswerId getId() { return id; }
    public void setId(AttemptAnswerId id) { this.id = id; }

    public Attempt getAttempt() { return attempt; }
    public void setAttempt(Attempt attempt) { this.attempt = attempt; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public QuestionOption getOption() { return option; }
    public void setOption(QuestionOption option) { this.option = option; }

    public OffsetDateTime getSelectedAt() { return selectedAt; }
    public void setSelectedAt(OffsetDateTime selectedAt) { this.selectedAt = selectedAt; }
}
