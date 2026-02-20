package com.quiz.dicom.repository;

import com.quiz.dicom.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByQuestionNoAsc(Long quizId);

}
