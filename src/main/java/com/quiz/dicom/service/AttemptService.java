package com.quiz.dicom.service;

import com.quiz.dicom.domain.*;
import com.quiz.dicom.dto.*;
import com.quiz.dicom.mapper.LeaderboardMapper;
import com.quiz.dicom.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        Map<Long, List<Long>> answers =
                (req == null || req.answers() == null) ? Map.of() : req.answers();

        Long quizId = attempt.getQuiz().getId();

        // Load quiz questions
        List<Question> questions =
                questionRepository.findByQuizIdOrderByQuestionNoAsc(quizId);

        if (questions.isEmpty()) {
            throw new IllegalStateException("Quiz has no questions: " + quizId);
        }

        // ===============================
        // Validation
        // ===============================
        for (Question q : questions) {

            List<Long> selected = answers.getOrDefault(q.getId(), List.of());
            if (selected == null) selected = List.of();

            if (q.isRequired() && selected.isEmpty()) {
                throw new IllegalArgumentException(
                        "Required question not answered: questionId=" + q.getId()
                );
            }

            if (q.getType() == QuestionType.SINGLE && selected.size() > 1) {
                throw new IllegalArgumentException(
                        "SINGLE question has multiple selections: questionId=" + q.getId()
                );
            }

            //  NEW: weighted questions must be SINGLE
            if (q.getScoringMode() == ScoringMode.WEIGHTED
                    && q.getType() != QuestionType.SINGLE) {
                throw new IllegalStateException(
                        "WEIGHTED scoring only supported for SINGLE questions: questionId=" + q.getId()
                );
            }
        }

        // ===============================
        // Remove previous answers (safe resubmit)
        // ===============================
        List<AttemptAnswer> existing =
                attemptAnswerRepository.findByIdAttemptId(attemptId);
        if (!existing.isEmpty()) {
            attemptAnswerRepository.deleteAll(existing);
        }

        // ===============================
        // Persist answers
        // ===============================
        for (Question q : questions) {

            List<Long> selectedOptionIds =
                    answers.getOrDefault(q.getId(), List.of());
            if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
                continue;
            }

            List<QuestionOption> options =
                    questionOptionRepository.findByQuestion_IdOrderByOptionNoAsc(q.getId());

            Map<Long, QuestionOption> optionById =
                    options.stream()
                            .collect(Collectors.toMap(QuestionOption::getId, o -> o));

            for (Long optId : selectedOptionIds) {
                QuestionOption opt = optionById.get(optId);
                if (opt == null) {
                    throw new IllegalArgumentException(
                            "Invalid option for question: questionId=" + q.getId() +
                                    ", optionId=" + optId
                    );
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

        // ===============================
        // Scoring
        // ===============================
        BigDecimal totalPoints = BigDecimal.ZERO;

        for (Question q : questions) {

            if (q.getScoringMode() == ScoringMode.WEIGHTED) {

                int maxOptionScore = questionOptionRepository
                        .findByQuestion_IdOrderByScoreDesc(q.getId())
                        .stream()
                        .findFirst()
                        .map(QuestionOption::getScore)
                        .orElse(0);

                totalPoints = totalPoints.add(BigDecimal.valueOf(maxOptionScore));

            } else { // BINARY
                totalPoints = totalPoints.add(
                        q.getPoints() == null ? BigDecimal.ZERO : q.getPoints()
                );
            }
        }


        BigDecimal score = BigDecimal.ZERO;

        for (Question q : questions) {

            List<Long> selected = answers.getOrDefault(q.getId(), List.of());
            if (selected == null || selected.isEmpty()) continue;

            Set<Long> selectedSet = new HashSet<>(selected);

            if (q.getScoringMode() == ScoringMode.WEIGHTED) {

                // SINGLE-choice weighted
                Long selectedOptionId = selectedSet.iterator().next();

                QuestionOption selectedOption =
                        questionOptionRepository.findById(selectedOptionId)
                                .orElseThrow(() ->
                                        new IllegalStateException("Option not found: " + selectedOptionId)
                                );

                score = score.add(BigDecimal.valueOf(selectedOption.getScore()));

            } else { // BINARY (existing logic)

                List<QuestionOption> correctOptions =
                        questionOptionRepository.findByQuestion_IdAndCorrectTrue(q.getId());

                Set<Long> correctSet = correctOptions.stream()
                        .map(QuestionOption::getId)
                        .collect(Collectors.toSet());

                boolean isCorrect =
                        q.getType() == QuestionType.SINGLE
                                ? selectedSet.size() == 1 && selectedSet.equals(correctSet)
                                : selectedSet.equals(correctSet);

                if (isCorrect) {
                    score = score.add(q.getPoints());
                }
            }
        }

        // ===============================
        // Finalize attempt
        // ===============================
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

    @Transactional(readOnly = true)
    public AttemptReviewDto getAttemptReview(Long attemptId, Long userId) {

        Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

        Quiz quiz = attempt.getQuiz();
        Long quizId = quiz.getId();

        List<Question> questions = questionRepository.findByQuizIdOrderByQuestionNoAsc(quizId);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        // 1) Selected options
        List<AttemptAnswer> selectedRows = attemptAnswerRepository.findByIdAttemptId(attemptId);

        Map<Long, List<Long>> selectedOptionIdsByQuestionId =
                selectedRows.stream()
                        .collect(Collectors.groupingBy(
                                aa -> aa.getId().getQuestionId(),
                                Collectors.mapping(aa -> aa.getId().getOptionId(), Collectors.toList())
                        ));

        // 2) All options
        List<QuestionOption> allOptions = questionOptionRepository.findByQuestion_IdIn(questionIds);

        Map<Long, List<QuestionOption>> optionsByQuestionIdRaw =
                allOptions.stream()
                        .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        Map<Long, List<OptionDto>> optionsByQuestionId =
                allOptions.stream()
                        .collect(Collectors.groupingBy(
                                o -> o.getQuestion().getId(),
                                Collectors.mapping(o -> {

                                    Question q = o.getQuestion();
                                    int score;

                                    if (q.getScoringMode() == ScoringMode.WEIGHTED) {
                                        // 5 / 3 / 1 / 0 from DB
                                        score = o.getScore();
                                    } else {
                                        // BINARY MCQ
                                        score = o.isCorrect()
                                                ? q.getPoints().intValue()
                                                : 0;
                                    }

                                    return new OptionDto(
                                            o.getId(),
                                            o.getOptionNo(),
                                            o.getText(),
                                            score
                                    );
                                }, Collectors.toList())
                        ));


        optionsByQuestionId.values().forEach(
                list -> list.sort(Comparator.comparing(OptionDto::optionNo))
        );

        // 3) Correct options
        Map<Long, Set<Long>> correctOptionIdsByQuestionId =
                allOptions.stream()
                        .filter(QuestionOption::isCorrect)
                        .collect(Collectors.groupingBy(
                                o -> o.getQuestion().getId(),
                                Collectors.mapping(QuestionOption::getId, Collectors.toSet())
                        ));

        // 4) Build review items
        List<AttemptReviewItemDto> items = new ArrayList<>(questions.size());

        for (Question q : questions) {

            List<Long> selected =
                    selectedOptionIdsByQuestionId.getOrDefault(q.getId(), List.of());

            Set<Long> selectedSet = new HashSet<>(selected);
            Set<Long> correctSet =
                    correctOptionIdsByQuestionId.getOrDefault(q.getId(), Set.of());

            BigDecimal achievedScore = BigDecimal.ZERO;
            BigDecimal maxScore = BigDecimal.ZERO;
            boolean isCorrect;

            List<QuestionOption> optionsForQuestion =
                    optionsByQuestionIdRaw.getOrDefault(q.getId(), List.of());

            if (q.getScoringMode() == ScoringMode.WEIGHTED) {

                // max score = highest option score (5)
                maxScore = BigDecimal.valueOf(
                        optionsForQuestion.stream()
                                .mapToInt(QuestionOption::getScore)
                                .max()
                                .orElse(0)
                );

                // achieved score = selected option score (5 / 3 / 1 / 0)
                if (!selected.isEmpty()) {
                    Long selectedOptionId = selected.get(0);
                    achievedScore = BigDecimal.valueOf(
                            optionsForQuestion.stream()
                                    .filter(o -> o.getId().equals(selectedOptionId))
                                    .map(QuestionOption::getScore)
                                    .findFirst()
                                    .orElse(0)
                    );
                }

                // ✅ CORRECTNESS RULE FOR WEIGHTED QUESTIONS
                isCorrect = achievedScore.compareTo(BigDecimal.ZERO) > 0;

            } else {
                // BINARY (MCQ) behavior
                maxScore = q.getPoints();

                isCorrect =
                        q.getType() == QuestionType.SINGLE
                                ? selectedSet.size() == 1 && selectedSet.equals(correctSet)
                                : selectedSet.equals(correctSet);

                achievedScore = isCorrect ? q.getPoints() : BigDecimal.ZERO;

                // ✅ IMPORTANT: derive option-level scores for UI summary
                for (QuestionOption o : optionsForQuestion) {
                    if (o.isCorrect()) {
                        o.setScore(q.getPoints().intValue()); // correct option = full marks
                    } else {
                        o.setScore(0); // incorrect option = 0
                    }
                }
            }

            items.add(new AttemptReviewItemDto(
                    q.getId(),
                    q.getQuestionNo(),
                    q.getType(),
                    q.getText(),
                    achievedScore.toPlainString(),
                    maxScore.toPlainString(),
                    q.isRequired(),
                    optionsByQuestionId.getOrDefault(q.getId(), List.of()),
                    selected,
                    new ArrayList<>(correctSet),
                    isCorrect
            ));
        }


        return new AttemptReviewDto(
                attempt.getId(),
                quizId,
                quiz.getTitle(),
                attempt.getStatus().name(),
                attempt.getScore() == null ? "0.00" : attempt.getScore().toPlainString(),
                attempt.getTotalPoints() == null ? "0.00" : attempt.getTotalPoints().toPlainString(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                items
        );
    }


    // Helper: get question id from QuestionOption depending on your entity mapping
    private Long questionIdOfOption(QuestionOption o) {
        // If your QuestionOption has a ManyToOne Question field:
        return o.getQuestion().getId();

        // If instead it has a questionId scalar:
        // return o.getQuestionId();
    }

    // Helper: correctness flag depending on your entity mapping
    private boolean isOptionCorrect(QuestionOption o) {
        // common patterns:
        return o.isCorrect();
        // or: return o.getIsCorrect();
        // or: return o.getCorrect();
    }
}
