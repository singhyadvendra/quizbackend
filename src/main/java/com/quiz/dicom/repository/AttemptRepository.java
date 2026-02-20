package com.quiz.dicom.repository;

import com.quiz.dicom.domain.Attempt;
import com.quiz.dicom.domain.AttemptStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByQuizIdAndStatusOrderByScoreDescSubmittedAtDesc(Long quizId, AttemptStatus status, Pageable pageable);

    Optional<Attempt> findByIdAndUser_Id(Long id, Long userId);

}
