package com.quiz.dicom.repository;

import com.quiz.dicom.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"questions", "questions.options"})
    Optional<Quiz> findWithQuestionsById(Long id);

    Optional<Quiz> findByIdAndActiveTrue(Long id);
}
