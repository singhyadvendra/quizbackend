package com.quiz.dicom.repository;

import com.quiz.dicom.domain.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderByOptionNoAsc(Long questionId);

    List<QuestionOption> findByQuestionIdAndCorrectTrue(Long questionId);
}
