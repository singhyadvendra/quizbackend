package com.quiz.dicom.service;

import com.quiz.dicom.domain.Question;
import com.quiz.dicom.domain.QuestionOption;
import com.quiz.dicom.domain.Quiz;
import com.quiz.dicom.dto.admin.*;
import com.quiz.dicom.repository.QuestionOptionRepository;
import com.quiz.dicom.repository.QuestionRepository;
import com.quiz.dicom.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AdminQuizService {

    private final AdminAuthorizationService adminAuthorizationService;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public AdminQuizService(AdminAuthorizationService adminAuthorizationService,
                            QuizRepository quizRepository,
                            QuestionRepository questionRepository,
                            QuestionOptionRepository optionRepository) {
        this.adminAuthorizationService = adminAuthorizationService;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Transactional
    public QuizAdminDto createQuiz(CreateQuizRequestDto req) {
        adminAuthorizationService.requireAdmin();

        Quiz quiz = new Quiz();
        quiz.setTitle(req.title());
        quiz.setDescription(req.description());
        quiz.setActive(req.active());
        quiz.setCreatedAt(OffsetDateTime.now());

        Quiz saved = quizRepository.save(quiz);

        return new QuizAdminDto(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.isActive(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public QuestionAdminDto addQuestion(Long quizId, CreateQuestionRequestDto req) {
        adminAuthorizationService.requireAdmin();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        Question q = new Question();
        q.setQuiz(quiz);
        q.setQuestionNo(req.questionNo());
        q.setType(req.type());               // <-- enum
        q.setText(req.text());
        q.setPoints(req.points());
        q.setRequired(req.required());
        q.setCreatedAt(OffsetDateTime.now());

        Question saved = questionRepository.save(q);

        return new QuestionAdminDto(
                saved.getId(),
                quiz.getId(),
                saved.getQuestionNo(),
                saved.getType(),              // <-- enum
                saved.getText(),
                saved.getPoints(),
                saved.isRequired(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public OptionAdminDto addOption(Long questionId, CreateOptionRequestDto req) {
        adminAuthorizationService.requireAdmin();

        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        QuestionOption opt = new QuestionOption();
        opt.setQuestion(q);
        opt.setOptionNo(req.optionNo());
        opt.setText(req.text());
        opt.setCorrect(req.correct());
        opt.setCreatedAt(OffsetDateTime.now());

        QuestionOption saved = optionRepository.save(opt);

        return new OptionAdminDto(
                saved.getId(),
                q.getId(),
                saved.getOptionNo(),
                saved.getText(),
                saved.isCorrect(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public QuizAdminDto setQuizActive(Long quizId, boolean active) {
        adminAuthorizationService.requireAdmin();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        quiz.setActive(active);
        Quiz saved = quizRepository.save(quiz);

        return new QuizAdminDto(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.isActive(),
                saved.getCreatedAt()
        );
    }
}
