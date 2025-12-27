package com.quiz.dicom.domain;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "question_option",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_option_question_no", columnNames = {"question_id", "option_no"}),
                @UniqueConstraint(name = "uq_option_question_id_id", columnNames = {"question_id", "id"})
        },
        indexes = {
                @Index(name = "ix_option_question_id", columnList = "question_id")
        }
)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many options belong to a question
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_no", nullable = false)
    private int optionNo;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // getters/setters

    public Long getId() { return id; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public int getOptionNo() { return optionNo; }
    public void setOptionNo(int optionNo) { this.optionNo = optionNo; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
