package com.quiz.dicom.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "attempt",
        indexes = {
                @Index(name = "ix_attempt_quiz_id", columnList = "quiz_id"),
                @Index(name = "ix_attempt_user_id", columnList = "user_id"),
                @Index(name = "ix_attempt_status", columnList = "status")
        }
)
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Attempt is for a quiz
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    // Attempt by a user (nullable if you ever allow anonymous)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "total_points", precision = 10, scale = 2)
    private BigDecimal totalPoints;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttemptAnswer> answers = new HashSet<>();

    // getters/setters

    public Long getId() { return id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal totalPoints) { this.totalPoints = totalPoints; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Set<AttemptAnswer> getAnswers() { return answers; }
    public void setAnswers(Set<AttemptAnswer> answers) { this.answers = answers; }
}
