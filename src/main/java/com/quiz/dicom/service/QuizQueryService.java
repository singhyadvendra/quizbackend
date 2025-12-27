package com.quiz.dicom.service;

import com.quiz.dicom.dto.QuestionDto;
import com.quiz.dicom.dto.QuizSummaryDto;
import com.quiz.dicom.mapper.QuestionMapper;
import com.quiz.dicom.mapper.QuizMapper;
import com.quiz.dicom.repository.QuestionRepository;
import com.quiz.dicom.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizQueryService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    public QuizQueryService(QuizRepository quizRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryDto> listActiveQuizzes() {
        return quizRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(QuizMapper::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> getQuizQuestions(Long quizId) {
        // Ensures quiz exists + is active
        quizRepository.findByIdAndActiveTrue(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found or inactive: " + quizId));

        // Load questions ordered; Question entity already has options relationship
        // If you prefer avoiding N+1, use a repository method with @EntityGraph.
        return questionRepository.findByQuizIdOrderByQuestionNoAsc(quizId)
                .stream()
                .map(QuestionMapper::toDto)
                .toList();
    }
}
