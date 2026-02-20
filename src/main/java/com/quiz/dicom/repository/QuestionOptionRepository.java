package com.quiz.dicom.repository;

import com.quiz.dicom.domain.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    // all options for one question
    List<QuestionOption> findByQuestion_IdOrderByOptionNoAsc(Long questionId);

    // correct options for one question
    List<QuestionOption> findByQuestion_IdAndCorrectTrue(Long questionId);

    // all options for many questions
    List<QuestionOption> findByQuestion_IdIn(Collection<Long> questionIds);

    List<QuestionOption> findByQuestion_IdOrderByScoreDesc(Long questionId);

}
