package com.quiz.dicom.service;

import com.quiz.dicom.domain.*;
import com.quiz.dicom.dto.*;
import com.quiz.dicom.mapper.LeaderboardMapper;
import com.quiz.dicom.repository.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttemptService {

    private final QuizRepository quizRepository;
    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AppUserRepository appUserRepository;

    public AttemptService(
            QuizRepository quizRepository,
            AttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository questionOptionRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            AppUserRepository appUserRepository
    ) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public StartAttemptResponseDto startAttempt(Long quizId, Long userId) {
        Quiz quiz = quizRepository.findByIdAndActiveTrue(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found or inactive: " + quizId));

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Attempt attempt = new Attempt();
        attempt.setQuiz(quiz);
        attempt.setUser(user);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setCreatedAt(OffsetDateTime.now());

        Attempt saved = attemptRepository.save(attempt);

        return new StartAttemptResponseDto(saved.getId(), quizId, saved.getStartedAt());
    }

    /**
     * Submit attempt:
     * - Validates ownership (attempt.user_id must match userId)
     * - Validates SINGLE/MULTI rules + required
     * - Stores one row per selected option in attempt_answer
     * - Scores:
     *    SINGLE: selected set equals correct set (size 1)
     *    MULTI: exact match selected set equals correct set
     */
    @Transactional
    public AttemptResultDto submitAttempt(Long attemptId, Long userId, SubmitAttemptRequestDto req) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        // Ownership enforcement
        if (attempt.getUser() == null || attempt.getUser().getId() == null) {
            throw new AccessDeniedException("Attempt is not associated with a user");
        }
        if (!attempt.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Attempt does not belong to current user");
        }

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new IllegalStateException("Attempt already submitted: " + attemptId);
        }

        OffsetDateTime submittedAt = (req != null && req.submittedAt() != null)
                ? req.submittedAt()
                : OffsetDateTime.now();

        Map<Long, List<Long>> answers = (req == null || req.answers() == null) ? Map.of() : req.answers();
        Long quizId = attempt.getQuiz().getId();

        // Load quiz questions
        List<Question> questions = questionRepository.findByQuizIdOrderByQuestionNoAsc(quizId);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Quiz has no questions: " + quizId);
        }

        // Validate required questions + SINGLE count rule
        for (Question q : questions) {
            List<Long> selected = answers.getOrDefault(q.getId(), List.of());
            if (selected == null) selected = List.of();

            if (q.isRequired() && selected.isEmpty()) {
                throw new IllegalArgumentException("Required question not answered: questionId=" + q.getId());
            }
            if (q.getType() == QuestionType.SINGLE && selected.size() > 1) {
                throw new IllegalArgumentException("SINGLE question has multiple selections: questionId=" + q.getId());
            }
        }

        // Remove previously saved answers (safe if user resubmits before final submit)
        List<AttemptAnswer> existing = attemptAnswerRepository.findByIdAttemptId(attemptId);
        if (!existing.isEmpty()) {
            attemptAnswerRepository.deleteAll(existing);
        }

        // Persist attempt_answer rows and validate that optionId belongs to questionId
        // (DB FK also enforces, but we validate to produce a clean error message)
        for (Question q : questions) {
            List<Long> selectedOptionIds = answers.getOrDefault(q.getId(), List.of());
            if (selectedOptionIds == null) selectedOptionIds = List.of();

            if (selectedOptionIds.isEmpty()) {
                continue; // unanswered optional
            }

            // Allowed options for this question
            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionNoAsc(q.getId());
            Map<Long, QuestionOption> optionById = options.stream()
                    .collect(Collectors.toMap(QuestionOption::getId, o -> o));

            for (Long optId : selectedOptionIds) {
                if (optId == null) continue;

                QuestionOption opt = optionById.get(optId);
                if (opt == null) {
                    throw new IllegalArgumentException("Invalid option for question: questionId=" + q.getId() + ", optionId=" + optId);
                }

                AttemptAnswer aa = new AttemptAnswer();
                aa.setAttempt(attempt);
                aa.setQuestion(q);
                aa.setOption(opt);
                aa.setSelectedAt(OffsetDateTime.now());
                aa.setId(new AttemptAnswerId(attempt.getId(), q.getId(), optId));

                attemptAnswerRepository.save(aa);
            }
        }

        // Compute total points (snapshot) + score (exact match)
        BigDecimal totalPoints = questions.stream()
                .map(Question::getPoints)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal score = BigDecimal.ZERO;

        for (Question q : questions) {
            BigDecimal points = (q.getPoints() == null) ? BigDecimal.ZERO : q.getPoints();

            List<Long> selected = answers.getOrDefault(q.getId(), List.of());
            if (selected == null) selected = List.of();

            Set<Long> selectedSet = new HashSet<>(selected);

            // correct options (server side)
            List<QuestionOption> correctOptions = questionOptionRepository.findByQuestionIdAndCorrectTrue(q.getId());
            Set<Long> correctSet = correctOptions.stream().map(QuestionOption::getId).collect(Collectors.toSet());

            boolean isCorrect;
            if (q.getType() == QuestionType.SINGLE) {
                // exact match with one correct option
                isCorrect = (selectedSet.size() == 1) && selectedSet.equals(correctSet);
            } else {
                // MULTI exact match
                isCorrect = selectedSet.equals(correctSet);
            }

            if (isCorrect) {
                score = score.add(points);
            }
        }

        // Finalize attempt
        attempt.setTotalPoints(totalPoints);
        attempt.setScore(score);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);

        Attempt saved = attemptRepository.save(attempt);

        return new AttemptResultDto(
                saved.getId(),
                saved.getQuiz().getId(),
                saved.getStatus().name(),
                saved.getScore() == null ? "0" : saved.getScore().toPlainString(),
                saved.getTotalPoints() == null ? "0" : saved.getTotalPoints().toPlainString(),
                saved.getStartedAt(),
                saved.getSubmittedAt()
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardResponseDto leaderboard(Long quizId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<Attempt> top = attemptRepository
                .findByQuizIdAndStatusOrderByScoreDescSubmittedAtDesc(
                        quizId,
                        AttemptStatus.SUBMITTED,
                        PageRequest.of(0, safeLimit)
                );

        return new LeaderboardResponseDto(
                quizId,
                LeaderboardMapper.toLeaderboard(top)
        );
    }
}
