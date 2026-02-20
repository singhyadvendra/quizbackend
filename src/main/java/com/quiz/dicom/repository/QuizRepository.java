package com.quiz.dicom.repository;

import com.quiz.dicom.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByActiveTrueOrderByCreatedAtDesc();

    Optional<Quiz> findByIdAndActiveTrue(Long id);
}
