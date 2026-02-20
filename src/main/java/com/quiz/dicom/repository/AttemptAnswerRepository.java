package com.quiz.dicom.repository;

import com.quiz.dicom.domain.AttemptAnswer;
import com.quiz.dicom.domain.AttemptAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, AttemptAnswerId> {

    List<AttemptAnswer> findByIdAttemptId(Long attemptId);

}
