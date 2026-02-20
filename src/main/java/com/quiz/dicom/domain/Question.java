package com.quiz.dicom.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "question",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_question_quiz_no", columnNames = {"quiz_id", "question_no"})
        },
        indexes = {
                @Index(name = "ix_question_quiz_id", columnList = "quiz_id")
        }
)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many questions belong to a quiz
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_no", nullable = false)
    private int questionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 16, nullable = false)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_mode", length = 16, nullable = false)
    private ScoringMode scoringMode = ScoringMode.BINARY;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "points", nullable = false, precision = 6, scale = 2)
    private java.math.BigDecimal points = java.math.BigDecimal.valueOf(1.00);

    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionNo ASC")
    private List<QuestionOption> options = new ArrayList<>();

    // getters/setters

    public Long getId() { return id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public int getQuestionNo() { return questionNo; }
    public void setQuestionNo(int questionNo) { this.questionNo = questionNo; }

    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }

    public ScoringMode getScoringMode() {   return scoringMode; }
    public void setScoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public java.math.BigDecimal getPoints() { return points; }
    public void setPoints(java.math.BigDecimal points) { this.points = points; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
